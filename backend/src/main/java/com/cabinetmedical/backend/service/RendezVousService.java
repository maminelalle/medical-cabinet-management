package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.CreneauResponse;
import com.cabinetmedical.backend.dto.RendezVousRequest;
import com.cabinetmedical.backend.dto.RendezVousResponse;
import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RendezVousService {
    private static final Set<StatutRendezVous> OUVERTS = EnumSet.of(StatutRendezVous.PLANIFIE, StatutRendezVous.CONFIRME);

    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final ConsultationRepository consultationRepository;
    private final FactureRepository factureRepository;
    private final PatientService patientService;
    private final DisponibiliteService disponibiliteService;
    private final UtilisateurConnecte utilisateurConnecte;

    @Transactional(readOnly = true)
    public List<RendezVousResponse> rechercher(Long medecinId, LocalDate date, LocalDate dateDebut, LocalDate dateFin,
                                               StatutRendezVous statut, UserDetails connecte) {
        Long medecinConnecte = utilisateurConnecte.medecinIdOuNull(connecte);
        Long medecinFiltre = medecinConnecte != null ? medecinConnecte : medecinId;

        LocalDateTime debut = date != null ? date.atStartOfDay() : (dateDebut == null ? null : dateDebut.atStartOfDay());
        LocalDateTime fin = date != null ? date.plusDays(1).atStartOfDay()
                : (dateFin == null ? null : dateFin.plusDays(1).atStartOfDay());
        return rendezVousRepository.findAllByOrderByDateHeureAsc().stream()
                .filter(rendezVous -> medecinFiltre == null || rendezVous.getMedecin().getId().equals(medecinFiltre))
                .filter(rendezVous -> debut == null || !rendezVous.getDateHeure().isBefore(debut))
                .filter(rendezVous -> fin == null || rendezVous.getDateHeure().isBefore(fin))
                .filter(rendezVous -> statut == null || rendezVous.getStatut() == statut)
                .map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public RendezVousResponse trouverRendezVous(Long id) {
        return versResponse(trouver(id));
    }

    @Transactional(readOnly = true)
    public List<CreneauResponse> creneaux(Long medecinId, LocalDate jour, Integer dureeMinutes) {
        trouverMedecin(medecinId);
        return disponibiliteService.creneaux(medecinId, jour, dureeMinutes == null ? 30 : dureeMinutes);
    }

    /** Cree le rendez-vous ; si {@code nouveauPatient} est fourni, la fiche patient est creee dans la meme transaction. */
    @Transactional
    public RendezVousResponse creer(RendezVousRequest request, UserDetails connecte) {
        Medecin medecin = trouverMedecin(request.medecinId());
        disponibiliteService.verifierDisponible(medecin.getId(), request.dateHeure(), request.dureeOuDefaut(), null, null);
        Patient patient = patientDeLaDemande(request);

        RendezVous rendezVous = new RendezVous();
        rendezVous.setPatient(patient);
        rendezVous.setMedecin(medecin);
        rendezVous.setDateHeure(request.dateHeure());
        rendezVous.setDureeMinutes(request.dureeOuDefaut());
        rendezVous.setMotif(request.motif());
        rendezVous.setNumeroFile(numeroFile(request.dateHeure().toLocalDate()));
        rendezVous.setCreatedBy(utilisateurConnecte.utilisateur(connecte));
        return versResponse(rendezVousRepository.save(rendezVous));
    }

    @Transactional
    public RendezVousResponse modifier(Long id, RendezVousRequest request) {
        RendezVous rendezVous = trouver(id);
        if (!OUVERTS.contains(rendezVous.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un rendez-vous planifié ou confirmé peut être modifié");
        }
        if (request.patientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient du rendez-vous est obligatoire");
        }
        Medecin medecin = trouverMedecin(request.medecinId());
        disponibiliteService.verifierDisponible(medecin.getId(), request.dateHeure(), request.dureeOuDefaut(), id, null);
        LocalDate nouveauJour = request.dateHeure().toLocalDate();
        if (!rendezVous.getDateHeure().toLocalDate().equals(nouveauJour)) {
            // Changement de jour : nouveau numero de file dans la journee cible.
            rendezVous.setNumeroFile(numeroFile(nouveauJour));
        }
        rendezVous.setPatient(trouverPatient(request.patientId()));
        rendezVous.setMedecin(medecin);
        rendezVous.setDateHeure(request.dateHeure());
        rendezVous.setDureeMinutes(request.dureeOuDefaut());
        rendezVous.setMotif(request.motif());
        return versResponse(rendezVousRepository.save(rendezVous));
    }

    /**
     * Cycle de vie : PLANIFIE -> CONFIRME -> EN_COURS (medecin) -> TERMINE (medecin).
     * ANNULE et ABSENT liberent le creneau. Un rendez-vous termine ou annule est clos.
     */
    @Transactional
    public RendezVousResponse changerStatut(Long id, StatutRendezVous statut, UserDetails connecte) {
        RendezVous rendezVous = trouver(id);
        boolean estMedecin = UtilisateurConnecte.aLeRole(connecte, "MEDECIN");
        if (estMedecin && !rendezVous.getMedecin().getId().equals(utilisateurConnecte.medecin(connecte).getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce rendez-vous ne concerne pas ce médecin");
        }
        StatutRendezVous actuel = rendezVous.getStatut();
        if (actuel == statut) return versResponse(rendezVous);
        if (actuel == StatutRendezVous.TERMINE || actuel == StatutRendezVous.ANNULE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous est clôturé : son statut ne peut plus changer");
        }
        switch (statut) {
            case EN_COURS -> {
                if (!estMedecin) refuser("Seul le médecin peut démarrer la consultation");
                if (!OUVERTS.contains(actuel)) refuser("Seul un rendez-vous planifié ou confirmé peut démarrer");
                boolean autreEnCours = rendezVousRepository.findAllByOrderByDateHeureAsc().stream()
                        .anyMatch(autre -> autre.getMedecin().getId().equals(rendezVous.getMedecin().getId())
                                && autre.getStatut() == StatutRendezVous.EN_COURS);
                if (autreEnCours) refuser("Terminez d'abord la consultation en cours avant d'en démarrer une autre");
                rendezVous.setDebutConsultation(Instant.now());
            }
            case TERMINE -> {
                if (!estMedecin) refuser("Seul le médecin peut terminer la consultation");
                if (actuel != StatutRendezVous.EN_COURS) refuser("Démarrez la consultation avant de la terminer");
                rendezVous.setFinConsultation(Instant.now());
            }
            case ANNULE -> {
                if (!OUVERTS.contains(actuel)) refuser("Seul un rendez-vous planifié ou confirmé peut être annulé");
                Facture facture = factureActive(rendezVous.getId());
                if (facture != null) {
                    refuser("Ce rendez-vous a la facture FAC-" + String.format("%03d", facture.getId())
                            + " : annulez d'abord la facture");
                }
            }
            case ABSENT -> {
                if (!OUVERTS.contains(actuel)) refuser("Seul un rendez-vous planifié ou confirmé peut être déclaré absent");
            }
            case PLANIFIE, CONFIRME -> {
                if (estMedecin) refuser("La confirmation des rendez-vous est faite par l'accueil");
                if (!OUVERTS.contains(actuel) && actuel != StatutRendezVous.ABSENT) refuser("Changement de statut impossible");
            }
        }
        rendezVous.setStatut(statut);
        return versResponse(rendezVousRepository.save(rendezVous));
    }

    /** Suppression definitive d'un rendez-vous sans consultation ni facture (sinon : l'annuler). */
    @Transactional
    public void supprimer(Long id) {
        RendezVous rendezVous = trouver(id);
        if (consultationRepository.existsByRendezVousId(id)) {
            refuser("Ce rendez-vous a une consultation : il ne peut pas être supprimé");
        }
        if (!factureRepository.findByRendezVousId(id).isEmpty()) {
            refuser("Ce rendez-vous est lié à une facture : annulez-le au lieu de le supprimer");
        }
        if (rendezVous.getStatut() == StatutRendezVous.EN_COURS || rendezVous.getStatut() == StatutRendezVous.TERMINE) {
            refuser("Un rendez-vous commencé ou terminé ne peut pas être supprimé");
        }
        rendezVousRepository.delete(rendezVous);
    }

    public RendezVousResponse versResponse(RendezVous rendezVous) {
        return RendezVousResponse.from(rendezVous, factureActive(rendezVous.getId()));
    }

    private Facture factureActive(Long rendezVousId) {
        return factureRepository.findByRendezVousId(rendezVousId).stream()
                .filter(facture -> facture.getStatut() != StatutFacture.ANNULEE)
                .findFirst().orElse(null);
    }

    private Patient patientDeLaDemande(RendezVousRequest request) {
        if (request.patientId() != null && request.nouveauPatient() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un patient existant ou créez-en un, pas les deux");
        }
        if (request.nouveauPatient() != null) {
            return patientRepository.findById(patientService.creer(request.nouveauPatient()).id()).orElseThrow();
        }
        if (request.patientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un patient ou saisissez un nouveau patient");
        }
        return trouverPatient(request.patientId());
    }

    private int numeroFile(LocalDate jour) {
        return (int) rendezVousRepository.countByDateHeureGreaterThanEqualAndDateHeureLessThan(
                jour.atStartOfDay(), jour.plusDays(1).atStartOfDay()) + 1;
    }

    private static void refuser(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private RendezVous trouver(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
    }

    private Patient trouverPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
    }

    private Medecin trouverMedecin(Long id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin introuvable"));
    }
}

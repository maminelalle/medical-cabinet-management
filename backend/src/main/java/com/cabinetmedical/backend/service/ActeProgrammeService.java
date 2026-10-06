package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ActeProgrammeRequest;
import com.cabinetmedical.backend.dto.ActeProgrammeResponse;
import com.cabinetmedical.backend.dto.MotifRequest;
import com.cabinetmedical.backend.dto.RealisationActeRequest;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.repository.ActeProgrammeRepository;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Actes programmes par le medecin (chirurgie, traitement, examen...) : planification, realisation avec
 * compte-rendu et resultat, ou annulation. L'accueil les voit pour organiser et facturer.
 */
@Service
@RequiredArgsConstructor
public class ActeProgrammeService {
    private final ActeProgrammeRepository acteProgrammeRepository;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;
    private final FactureRepository factureRepository;
    private final DisponibiliteService disponibiliteService;
    private final UtilisateurConnecte utilisateurConnecte;

    @Transactional(readOnly = true)
    public List<ActeProgrammeResponse> lister(Long patientId, StatutActe statut, UserDetails connecte) {
        Long medecinId = utilisateurConnecte.medecinIdOuNull(connecte);
        List<ActeProgramme> actes = patientId != null ? acteProgrammeRepository.findByPatientIdOrderByDateHeureDesc(patientId)
                : medecinId != null ? acteProgrammeRepository.findByMedecinIdOrderByDateHeureDesc(medecinId)
                : acteProgrammeRepository.findAllByOrderByDateHeureDesc();
        return actes.stream()
                .filter(acte -> medecinId == null || acte.getMedecin().getId().equals(medecinId))
                .filter(acte -> statut == null || acte.getStatut() == statut)
                .map(acte -> versResponse(acte, connecte)).toList();
    }

    @Transactional(readOnly = true)
    public ActeProgrammeResponse trouver(Long id, UserDetails connecte) {
        ActeProgramme acte = trouverActe(id);
        Long medecinId = utilisateurConnecte.medecinIdOuNull(connecte);
        if (medecinId != null && !acte.getMedecin().getId().equals(medecinId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet acte ne concerne pas ce médecin");
        }
        return versResponse(acte, connecte);
    }

    @Transactional
    public ActeProgrammeResponse programmer(ActeProgrammeRequest request, UserDetails connecte) {
        Medecin medecin = utilisateurConnecte.medecin(connecte);
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
        boolean suitLePatient = rendezVousRepository.findByPatientIdOrderByDateHeureDesc(patient.getId()).stream()
                .anyMatch(rendezVous -> rendezVous.getMedecin().getId().equals(medecin.getId()));
        if (!suitLePatient) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce patient ne fait pas partie de vos patients");
        }
        verifierDateFuture(request.dateHeure());
        disponibiliteService.verifierDisponible(medecin.getId(), request.dateHeure(), request.dureeOuDefaut(), null, null);

        ActeProgramme acte = new ActeProgramme();
        acte.setPatient(patient);
        acte.setMedecin(medecin);
        acte.setCreatedBy(utilisateurConnecte.utilisateur(connecte));
        if (request.consultationId() != null) {
            Consultation consultation = consultationRepository.findById(request.consultationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
            if (!consultation.getRendezVous().getPatient().getId().equals(patient.getId())
                    || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La consultation ne correspond pas à ce patient et à ce médecin");
            }
            acte.setConsultation(consultation);
        }
        appliquer(acte, request);
        return versResponse(acteProgrammeRepository.save(acte), connecte);
    }

    @Transactional
    public ActeProgrammeResponse modifier(Long id, ActeProgrammeRequest request, UserDetails connecte) {
        ActeProgramme acte = acteDuMedecin(id, connecte);
        exigerPlanifie(acte);
        if (!acte.getDateHeure().equals(request.dateHeure())) verifierDateFuture(request.dateHeure());
        disponibiliteService.verifierDisponible(acte.getMedecin().getId(), request.dateHeure(), request.dureeOuDefaut(), null, acte.getId());
        appliquer(acte, request);
        return versResponse(acteProgrammeRepository.save(acte), connecte);
    }

    /** Enregistre la realisation : resultat, compte-rendu detaille et date effective. */
    @Transactional
    public ActeProgrammeResponse realiser(Long id, RealisationActeRequest request, UserDetails connecte) {
        ActeProgramme acte = acteDuMedecin(id, connecte);
        exigerPlanifie(acte);
        LocalDateTime date = request.dateRealisation() != null ? request.dateRealisation() : LocalDateTime.now();
        if (date.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de réalisation ne peut pas être dans le futur");
        }
        acte.setStatut(StatutActe.REALISE);
        acte.setResultat(request.resultat());
        acte.setCompteRendu(request.compteRendu().trim());
        acte.setDateRealisation(date);
        return versResponse(acteProgrammeRepository.save(acte), connecte);
    }

    /** Annulation par le medecin auteur ou par l'accueil, refusee si une facture active existe. */
    @Transactional
    public ActeProgrammeResponse annuler(Long id, MotifRequest request, UserDetails connecte) {
        ActeProgramme acte = UtilisateurConnecte.aLeRole(connecte, "MEDECIN") ? acteDuMedecin(id, connecte) : trouverActe(id);
        exigerPlanifie(acte);
        if (factureActive(acte) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet acte est déjà facturé : annulez d'abord la facture");
        }
        acte.setStatut(StatutActe.ANNULE);
        acte.setMotifAnnulation(request.motif().trim());
        return versResponse(acteProgrammeRepository.save(acte), connecte);
    }

    public ActeProgrammeResponse versResponse(ActeProgramme acte, UserDetails connecte) {
        boolean masquer = UtilisateurConnecte.aLeRole(connecte, "ACCUEIL") || UtilisateurConnecte.aLeRole(connecte, "ADMIN");
        return ActeProgrammeResponse.from(acte, factureActive(acte), masquer);
    }

    private Facture factureActive(ActeProgramme acte) {
        return factureRepository.findByActeProgrammeId(acte.getId()).stream()
                .filter(facture -> facture.getStatut() != StatutFacture.ANNULEE).findFirst().orElse(null);
    }

    private void appliquer(ActeProgramme acte, ActeProgrammeRequest request) {
        acte.setType(request.type());
        acte.setIntitule(request.intitule().trim());
        acte.setDetails(request.details());
        acte.setDateHeure(request.dateHeure());
        acte.setDureeMinutes(request.dureeOuDefaut());
        acte.setLieu(request.lieu());
    }

    private static void verifierDateFuture(LocalDateTime dateHeure) {
        if (dateHeure.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de l'acte doit être dans le futur");
        }
    }

    private static void exigerPlanifie(ActeProgramme acte) {
        if (acte.getStatut() != StatutActe.PLANIFIE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet acte est déjà " + (acte.getStatut() == StatutActe.REALISE ? "réalisé" : "annulé"));
        }
    }

    private ActeProgramme acteDuMedecin(Long id, UserDetails connecte) {
        ActeProgramme acte = trouverActe(id);
        if (!acte.getMedecin().getId().equals(utilisateurConnecte.medecin(connecte).getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet acte ne concerne pas ce médecin");
        }
        return acte;
    }

    private ActeProgramme trouverActe(Long id) {
        return acteProgrammeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte programmé introuvable"));
    }
}

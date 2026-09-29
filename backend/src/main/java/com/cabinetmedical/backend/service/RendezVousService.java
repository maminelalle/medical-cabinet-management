package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.RendezVousRequest;
import com.cabinetmedical.backend.dto.RendezVousResponse;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RendezVousService {
    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<RendezVousResponse> rechercher(Long medecinId, LocalDate date, StatutRendezVous statut,
                                               UserDetails utilisateurConnecte) {
        Long medecinFiltre = medecinId;
        if (estMedecin(utilisateurConnecte)) {
            medecinFiltre = trouverMedecinConnecte(utilisateurConnecte).getId();
        }
        Long medecinFiltreFinal = medecinFiltre;

        LocalDateTime dateDebut = date == null ? null : date.atStartOfDay();
        LocalDateTime dateFin = date == null ? null : date.plusDays(1).atStartOfDay();
        return rendezVousRepository.findAllByOrderByDateHeureAsc().stream()
            .filter(rendezVous -> medecinFiltreFinal == null || rendezVous.getMedecin().getId().equals(medecinFiltreFinal))
            .filter(rendezVous -> dateDebut == null || !rendezVous.getDateHeure().isBefore(dateDebut))
            .filter(rendezVous -> dateFin == null || rendezVous.getDateHeure().isBefore(dateFin))
            .filter(rendezVous -> statut == null || rendezVous.getStatut() == statut)
                .map(RendezVousResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RendezVousResponse trouverRendezVous(Long id) {
        return RendezVousResponse.from(trouver(id));
    }

    @Transactional
    public RendezVousResponse creer(RendezVousRequest request, UserDetails utilisateurConnecte) {
        Patient patient = trouverPatient(request.patientId());
        Medecin medecin = trouverMedecin(request.medecinId());
        verifierCreneauDisponible(medecin.getId(), request.dateHeure(), null);

        RendezVous rendezVous = new RendezVous();
        rendezVous.setPatient(patient);
        rendezVous.setMedecin(medecin);
        rendezVous.setDateHeure(request.dateHeure());
        rendezVous.setMotif(request.motif());
        rendezVous.setCreatedBy(trouverUtilisateur(utilisateurConnecte));
        return RendezVousResponse.from(rendezVousRepository.save(rendezVous));
    }

    @Transactional
    public RendezVousResponse modifier(Long id, RendezVousRequest request) {
        RendezVous rendezVous = trouver(id);
        Medecin medecin = trouverMedecin(request.medecinId());
        verifierCreneauDisponible(medecin.getId(), request.dateHeure(), id);
        rendezVous.setPatient(trouverPatient(request.patientId()));
        rendezVous.setMedecin(medecin);
        rendezVous.setDateHeure(request.dateHeure());
        rendezVous.setMotif(request.motif());
        return RendezVousResponse.from(rendezVousRepository.save(rendezVous));
    }

    @Transactional
    public RendezVousResponse changerStatut(Long id, StatutRendezVous statut, UserDetails utilisateurConnecte) {
        RendezVous rendezVous = trouver(id);
        if (estMedecin(utilisateurConnecte)
                && !rendezVous.getMedecin().getUtilisateur().getEmail().equalsIgnoreCase(utilisateurConnecte.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce rendez-vous ne concerne pas ce médecin");
        }
        rendezVous.setStatut(statut);
        return RendezVousResponse.from(rendezVousRepository.save(rendezVous));
    }

    private void verifierCreneauDisponible(Long medecinId, LocalDateTime dateHeure, Long rendezVousId) {
        boolean occupe = rendezVousRepository.existsByMedecinIdAndDateHeureAndStatutNot(
                medecinId, dateHeure, StatutRendezVous.ANNULE);
        if (occupe && (rendezVousId == null || !rendezVousRepository.findById(rendezVousId)
                .map(rendezVous -> rendezVous.getMedecin().getId().equals(medecinId)
                        && rendezVous.getDateHeure().equals(dateHeure))
                .orElse(false))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le créneau du médecin est déjà occupé");
        }
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

    private Utilisateur trouverUtilisateur(UserDetails userDetails) {
        return utilisateurRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private Medecin trouverMedecinConnecte(UserDetails userDetails) {
        return medecinRepository.findByUtilisateurId(trouverUtilisateur(userDetails).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Profil médecin introuvable"));
    }

    private boolean estMedecin(UserDetails userDetails) {
        return userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEDECIN"));
    }
}
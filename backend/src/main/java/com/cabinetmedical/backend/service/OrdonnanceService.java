package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.OrdonnanceResponse;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Prescription;
import com.cabinetmedical.backend.repository.DispensationRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Consultation et impression des ordonnances. Un medecin ne voit que les ordonnances qu'il a redigees ;
 * l'accueil, la pharmacie et la direction voient toutes les ordonnances du cabinet.
 */
@Service
@RequiredArgsConstructor
public class OrdonnanceService {
    private final PrescriptionRepository prescriptionRepository;
    private final DispensationRepository dispensationRepository;
    private final MedecinRepository medecinRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<OrdonnanceResponse> lister(Long patientId, UserDetails utilisateurConnecte) {
        Long medecinId = medecinConnecteId(utilisateurConnecte);
        return prescriptionRepository.findAllByOrderByDatePrescriptionDesc().stream()
                .filter(prescription -> patientId == null
                        || prescription.getConsultation().getRendezVous().getPatient().getId().equals(patientId))
                .filter(prescription -> medecinId == null
                        || prescription.getConsultation().getRendezVous().getMedecin().getId().equals(medecinId))
                .map(this::versResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdonnanceResponse trouver(Long id, UserDetails utilisateurConnecte) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable"));
        Long medecinId = medecinConnecteId(utilisateurConnecte);
        if (medecinId != null && !prescription.getConsultation().getRendezVous().getMedecin().getId().equals(medecinId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette ordonnance ne concerne pas ce médecin");
        }
        return versResponse(prescription);
    }

    private OrdonnanceResponse versResponse(Prescription prescription) {
        return OrdonnanceResponse.from(prescription, dispensationRepository.existsByPrescriptionId(prescription.getId()));
    }

    /** Identifiant du medecin connecte, ou null si l'utilisateur n'est pas medecin. */
    private Long medecinConnecteId(UserDetails utilisateurConnecte) {
        boolean estMedecin = utilisateurConnecte.getAuthorities().stream()
                .anyMatch(authorite -> authorite.getAuthority().equals("ROLE_MEDECIN"));
        if (!estMedecin) {
            return null;
        }
        return utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername())
                .flatMap(utilisateur -> medecinRepository.findByUtilisateurId(utilisateur.getId()))
                .map(Medecin::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Profil médecin introuvable"));
    }
}

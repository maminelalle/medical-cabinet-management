package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ConsultationDossierResponse;
import com.cabinetmedical.backend.dto.DossierPatientResponse;
import com.cabinetmedical.backend.dto.PatientResponse;
import com.cabinetmedical.backend.dto.PrescriptionResponse;
import com.cabinetmedical.backend.dto.RendezVousResponse;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Dossier medical consulte par le medecin : historique des consultations, des prescriptions
 * et prochains rendez-vous du patient.
 */
@Service
@RequiredArgsConstructor
public class DossierService {
    private final PatientRepository patientRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final MedecinRepository medecinRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public DossierPatientResponse dossier(Long patientId, UserDetails medecinConnecte) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

        List<RendezVous> rendezVousDuPatient = rendezVousRepository.findByPatientIdOrderByDateHeureDesc(patientId);
        verifierAcces(rendezVousDuPatient, medecinConnecte);

        List<ConsultationDossierResponse> consultations = consultationRepository
                .findByPatientIdOrderByDateHeureDesc(patientId).stream()
                .map(consultation -> ConsultationDossierResponse.from(consultation,
                        prescriptionRepository.findByConsultationId(consultation.getId())
                                .map(PrescriptionResponse::from).orElse(null)))
                .toList();

        LocalDateTime maintenant = LocalDateTime.now();
        List<RendezVousResponse> prochains = rendezVousDuPatient.stream()
                .filter(rendezVous -> !rendezVous.getDateHeure().isBefore(maintenant))
                .sorted(Comparator.comparing(RendezVous::getDateHeure))
                .limit(10)
                .map(RendezVousResponse::from)
                .toList();

        return new DossierPatientResponse(PatientResponse.from(patient), consultations, prochains);
    }

    /**
     * Principe du moindre privilege : un medecin n'accede qu'aux patients avec lesquels il a un rendez-vous.
     */
    private void verifierAcces(List<RendezVous> rendezVousDuPatient, UserDetails utilisateurConnecte) {
        boolean estMedecin = utilisateurConnecte.getAuthorities().stream()
                .anyMatch(authorite -> authorite.getAuthority().equals("ROLE_MEDECIN"));
        if (!estMedecin) {
            return;
        }
        Medecin medecin = medecinRepository
                .findByUtilisateurId(utilisateurRepository
                        .findByEmailIgnoreCase(utilisateurConnecte.getUsername())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"))
                        .getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Profil médecin introuvable"));
        boolean suitLePatient = rendezVousDuPatient.stream()
                .anyMatch(rendezVous -> rendezVous.getMedecin().getId().equals(medecin.getId()));
        if (!suitLePatient) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce dossier ne concerne pas ce médecin");
        }
    }
}
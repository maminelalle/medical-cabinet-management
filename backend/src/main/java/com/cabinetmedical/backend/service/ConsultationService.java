package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ConsultationRequest;
import com.cabinetmedical.backend.dto.ConsultationResponse;
import com.cabinetmedical.backend.dto.PrescriptionRequest;
import com.cabinetmedical.backend.dto.PrescriptionResponse;
import com.cabinetmedical.backend.entity.Consultation;
import com.cabinetmedical.backend.entity.LignePrescription;
import com.cabinetmedical.backend.entity.Prescription;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.LignePrescriptionRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConsultationService {
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final RendezVousRepository rendezVousRepository;

    @Transactional
    public ConsultationResponse creer(Long rendezVousId, ConsultationRequest request, UserDetails medecinConnecte) {
        RendezVous rendezVous = trouverRendezVous(rendezVousId);
        verifierAcces(rendezVous, medecinConnecte);
        if (consultationRepository.existsByRendezVousId(rendezVousId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une consultation existe déjà pour ce rendez-vous");
        }
        if (rendezVous.getStatut() == StatutRendezVous.ANNULE || rendezVous.getStatut() == StatutRendezVous.ABSENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous ne peut pas donner lieu à une consultation");
        }
        Consultation consultation = new Consultation();
        consultation.setRendezVous(rendezVous);
        consultation.setCompteRendu(request.compteRendu());
        rendezVous.setStatut(StatutRendezVous.TERMINE);
        return ConsultationResponse.from(consultationRepository.save(consultation));
    }

    @Transactional(readOnly = true)
    public ConsultationResponse trouver(Long id, UserDetails medecinConnecte) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
        verifierAcces(consultation.getRendezVous(), medecinConnecte);
        return ConsultationResponse.from(consultation);
    }

    @Transactional
    public PrescriptionResponse ajouterPrescription(Long consultationId, PrescriptionRequest request,
                                                     UserDetails medecinConnecte) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable"));
        verifierAcces(consultation.getRendezVous(), medecinConnecte);
        if (prescriptionRepository.existsByConsultationId(consultationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une prescription existe déjà pour cette consultation");
        }
        Prescription prescription = new Prescription();
        prescription.setConsultation(consultation);
        prescription.setDatePrescription(request.datePrescription());
        prescription.setInstructions(request.instructions());
        request.lignes().forEach(item -> {
            LignePrescription ligne = new LignePrescription();
            ligne.setPrescription(prescription);
            ligne.setMedicament(item.medicament());
            ligne.setPosologie(item.posologie());
            ligne.setDuree(item.duree());
            prescription.getLignes().add(ligne);
        });
        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    private RendezVous trouverRendezVous(Long id) {
        return rendezVousRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
    }

    private void verifierAcces(RendezVous rendezVous, UserDetails medecinConnecte) {
        if (!rendezVous.getMedecin().getUtilisateur().getEmail().equalsIgnoreCase(medecinConnecte.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce dossier ne concerne pas ce médecin");
        }
    }
}
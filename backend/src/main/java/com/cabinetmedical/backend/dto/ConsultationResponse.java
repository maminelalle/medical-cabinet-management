package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Consultation;

import java.time.Instant;

public record ConsultationResponse(Long id, Long rendezVousId, Long patientId, String patientNom,
                                   String patientPrenom, Long medecinId, String compteRendu, Instant createdAt) {
    public static ConsultationResponse from(Consultation consultation) {
        var rendezVous = consultation.getRendezVous();
        return new ConsultationResponse(
                consultation.getId(), rendezVous.getId(), rendezVous.getPatient().getId(),
                rendezVous.getPatient().getNom(), rendezVous.getPatient().getPrenom(),
                rendezVous.getMedecin().getId(), consultation.getCompteRendu(), consultation.getCreatedAt());
    }
}
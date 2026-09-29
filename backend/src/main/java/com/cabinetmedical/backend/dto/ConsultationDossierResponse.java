package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Consultation;

import java.time.LocalDateTime;

public record ConsultationDossierResponse(
        Long id,
        Long rendezVousId,
        LocalDateTime dateHeure,
        String motif,
        Long medecinId,
        String medecinNom,
        String medecinPrenom,
        String specialite,
        String compteRendu,
        PrescriptionResponse prescription
) {
    public static ConsultationDossierResponse from(Consultation consultation, PrescriptionResponse prescription) {
        var rendezVous = consultation.getRendezVous();
        return new ConsultationDossierResponse(
                consultation.getId(), rendezVous.getId(), rendezVous.getDateHeure(), rendezVous.getMotif(),
                rendezVous.getMedecin().getId(), rendezVous.getMedecin().getNom(), rendezVous.getMedecin().getPrenom(),
                rendezVous.getMedecin().getSpecialite(), consultation.getCompteRendu(), prescription);
    }
}
package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Prescription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Ordonnance complete, prete a etre consultee ou imprimee (patient, medecin, lignes, statut de delivrance). */
public record OrdonnanceResponse(
        Long id,
        Long consultationId,
        Long rendezVousId,
        LocalDateTime dateConsultation,
        Long patientId,
        String patientNom,
        String patientPrenom,
        LocalDate patientDateNaissance,
        Long medecinId,
        String medecinNom,
        String medecinPrenom,
        String specialite,
        String numeroOrdre,
        LocalDate datePrescription,
        String instructions,
        boolean delivree,
        List<LignePrescriptionResponse> lignes
) {
    public static OrdonnanceResponse from(Prescription prescription, boolean delivree) {
        var rendezVous = prescription.getConsultation().getRendezVous();
        var patient = rendezVous.getPatient();
        var medecin = rendezVous.getMedecin();
        return new OrdonnanceResponse(
                prescription.getId(), prescription.getConsultation().getId(), rendezVous.getId(), rendezVous.getDateHeure(),
                patient.getId(), patient.getNom(), patient.getPrenom(), patient.getDateNaissance(),
                medecin.getId(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite(), medecin.getNumeroOrdre(),
                prescription.getDatePrescription(), prescription.getInstructions(), delivree,
                prescription.getLignes().stream().map(LignePrescriptionResponse::from).toList());
    }
}

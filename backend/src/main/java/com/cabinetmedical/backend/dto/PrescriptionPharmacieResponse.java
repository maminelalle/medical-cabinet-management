package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Prescription;
import java.time.LocalDate;
import java.util.List;

public record PrescriptionPharmacieResponse(Long id, Long patientId, String patientNom, String patientPrenom,
                                            LocalDate datePrescription, String instructions,
                                            List<LignePrescriptionResponse> lignes) {
    public static PrescriptionPharmacieResponse from(Prescription prescription) {
        var patient = prescription.getConsultation().getRendezVous().getPatient();
        return new PrescriptionPharmacieResponse(prescription.getId(), patient.getId(), patient.getNom(), patient.getPrenom(),
                prescription.getDatePrescription(), prescription.getInstructions(),
                prescription.getLignes().stream().map(LignePrescriptionResponse::from).toList());
    }
}
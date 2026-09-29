package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Prescription;

import java.time.LocalDate;
import java.util.List;

public record PrescriptionResponse(Long id, Long consultationId, LocalDate datePrescription,
                                   String instructions, List<LignePrescriptionResponse> lignes) {
    public static PrescriptionResponse from(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(), prescription.getConsultation().getId(), prescription.getDatePrescription(),
                prescription.getInstructions(), prescription.getLignes().stream().map(LignePrescriptionResponse::from).toList());
    }
}
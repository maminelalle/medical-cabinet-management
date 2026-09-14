package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Patient;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String telephone,
        String email,
        String adresse
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getNom(), patient.getPrenom(),
                patient.getDateNaissance(), patient.getTelephone(), patient.getEmail(), patient.getAdresse());
    }
}

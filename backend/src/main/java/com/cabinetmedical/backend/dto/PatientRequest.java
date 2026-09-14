package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotNull LocalDate dateNaissance,
        String telephone,
        @Email String email,
        String adresse
) {
}

package com.cabinetmedical.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PrescriptionRequest(
        @NotNull LocalDate datePrescription,
        String instructions,
        @NotEmpty List<@Valid LignePrescriptionRequest> lignes
) {}
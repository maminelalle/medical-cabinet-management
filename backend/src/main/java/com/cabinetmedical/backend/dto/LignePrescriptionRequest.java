package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LignePrescriptionRequest(
        Long medicamentId,
        @NotBlank @Size(max = 255) String medicament,
        @Size(max = 255) String posologie,
        @Size(max = 100) String duree
) {}
package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Tarif du catalogue. {@code specialite} renseignee = tarif de consultation de cette specialite. */
public record CatalogueActeRequest(
        @NotBlank @Size(max = 150) String libelle,
        @NotBlank @Size(max = 50) String type,
        @NotNull @DecimalMin("0.00") BigDecimal montantDefaut,
        @Size(max = 150) String specialite,
        @Size(max = 255) String description,
        Boolean actif
) {}

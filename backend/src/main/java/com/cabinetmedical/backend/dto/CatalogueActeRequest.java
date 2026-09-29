package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CatalogueActeRequest(
        @NotBlank @Size(max = 150) String libelle,
        @NotBlank @Size(max = 50) String type,
        @NotNull @DecimalMin("0.00") BigDecimal montantDefaut
) {}
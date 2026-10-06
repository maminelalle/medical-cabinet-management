package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicamentRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 100) String dosage,
        @Size(max = 100) String forme,
        @Size(max = 100) String famille,
        @NotNull @PositiveOrZero Integer stockActuel,
        @NotNull @PositiveOrZero Integer seuilAlerte,
        @NotNull @PositiveOrZero BigDecimal prixAchat,
        @NotNull @PositiveOrZero BigDecimal prixVente,
        @Size(max = 150) String fournisseur,
        LocalDate dateExpiration) { }

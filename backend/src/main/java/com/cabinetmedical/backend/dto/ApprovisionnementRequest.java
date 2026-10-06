package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Achat aupres d'un fournisseur : entree en stock et cout d'achat. */
public record ApprovisionnementRequest(
        @NotNull @Positive Integer quantite,
        @NotNull @PositiveOrZero BigDecimal prixAchatUnitaire,
        @Size(max = 150) String fournisseur,
        @Size(max = 100) String reference,
        LocalDate dateExpiration
) {}

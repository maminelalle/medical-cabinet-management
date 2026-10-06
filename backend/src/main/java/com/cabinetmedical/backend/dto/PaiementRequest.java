package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Paiement : la reference de transaction est obligatoire pour tout moyen autre que les especes. */
public record PaiementRequest(
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        @NotBlank @Size(max = 50) String moyenPaiement,
        @Size(max = 100) String reference
) {}

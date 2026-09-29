package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaiementRequest(
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        @Size(max = 50) String moyenPaiement
) {}
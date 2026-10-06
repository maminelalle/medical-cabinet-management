package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Correction d'inventaire : nouveau stock compte, avec la raison de l'ecart. */
public record StockRequest(@NotNull @PositiveOrZero Integer stockActuel, @Size(max = 255) String commentaire) { }

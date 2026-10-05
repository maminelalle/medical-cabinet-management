package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DispensationLineRequest(@NotNull Long medicamentId, @NotNull @Positive Integer quantite) { }
package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(@NotNull @PositiveOrZero Integer stockActuel) { }
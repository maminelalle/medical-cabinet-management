package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Rendez-vous de controle programme par le medecin a la fin d'une consultation. */
public record ControleRequest(
        @NotNull LocalDateTime dateHeure,
        @Min(5) @Max(480) Integer dureeMinutes,
        @Size(max = 500) String motif
) {}

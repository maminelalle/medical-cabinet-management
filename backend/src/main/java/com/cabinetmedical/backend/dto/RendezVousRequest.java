package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RendezVousRequest(
        @NotNull Long patientId,
        @NotNull Long medecinId,
        @NotNull LocalDateTime dateHeure,
        @Size(max = 500) String motif
) {}
package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.StatutRendezVous;
import jakarta.validation.constraints.NotNull;

public record StatutRendezVousRequest(@NotNull StatutRendezVous statut) {}
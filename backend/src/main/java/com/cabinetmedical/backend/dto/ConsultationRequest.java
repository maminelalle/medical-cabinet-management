package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationRequest(@NotBlank @Size(max = 10000) String compteRendu) {}
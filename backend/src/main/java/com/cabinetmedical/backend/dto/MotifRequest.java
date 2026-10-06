package com.cabinetmedical.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotifRequest(@NotBlank @Size(max = 500) String motif) {}

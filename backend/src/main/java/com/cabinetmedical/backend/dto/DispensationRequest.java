package com.cabinetmedical.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DispensationRequest(@NotNull Long prescriptionId,
                                  @NotEmpty List<@Valid DispensationLineRequest> lignes) { }
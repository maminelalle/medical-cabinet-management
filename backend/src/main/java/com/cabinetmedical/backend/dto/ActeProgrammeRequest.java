package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.TypeActe;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ActeProgrammeRequest(
        @NotNull Long patientId,
        Long consultationId,
        @NotNull TypeActe type,
        @NotBlank @Size(max = 200) String intitule,
        @Size(max = 5000) String details,
        @NotNull LocalDateTime dateHeure,
        @Min(5) @Max(1440) Integer dureeMinutes,
        @Size(max = 100) String lieu
) {
    public int dureeOuDefaut() { return dureeMinutes == null ? 60 : dureeMinutes; }
}

package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Dispensation;
import java.time.Instant;
import java.util.List;

public record DispensationResponse(Long id, Long prescriptionId, Long patientId, Long factureId, Instant dateDispensation,
                                   List<MedicamentResponse> medicaments) {
    public static DispensationResponse from(Dispensation item) {
        return new DispensationResponse(item.getId(), item.getPrescription().getId(), item.getPatient().getId(), null,
                item.getDateDispensation(), item.getLignes().stream().map(line -> MedicamentResponse.from(line.getMedicament())).toList());
    }
}
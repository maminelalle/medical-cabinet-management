package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.LignePrescription;

public record LignePrescriptionResponse(Long id, Long medicamentId, String medicament, String posologie, String duree) {
    public static LignePrescriptionResponse from(LignePrescription ligne) {
        return new LignePrescriptionResponse(ligne.getId(), ligne.getMedicamentReference() == null ? null : ligne.getMedicamentReference().getId(),
                ligne.getMedicament(), ligne.getPosologie(), ligne.getDuree());
    }
}
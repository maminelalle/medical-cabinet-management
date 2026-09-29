package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.LignePrescription;

public record LignePrescriptionResponse(Long id, String medicament, String posologie, String duree) {
    public static LignePrescriptionResponse from(LignePrescription ligne) {
        return new LignePrescriptionResponse(ligne.getId(), ligne.getMedicament(), ligne.getPosologie(), ligne.getDuree());
    }
}
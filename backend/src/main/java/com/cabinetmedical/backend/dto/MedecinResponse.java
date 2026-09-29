package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Medecin;

public record MedecinResponse(Long id, String nom, String prenom, String specialite) {
    public static MedecinResponse from(Medecin medecin) {
        return new MedecinResponse(medecin.getId(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite());
    }
}
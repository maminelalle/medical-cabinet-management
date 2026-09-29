package com.cabinetmedical.backend.dto;

public record MedecinActiviteResponse(
        Long medecinId,
        String nom,
        String prenom,
        String specialite,
        long rendezVous,
        long consultations
) {}
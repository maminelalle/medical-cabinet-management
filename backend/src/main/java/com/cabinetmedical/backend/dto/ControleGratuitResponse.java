package com.cabinetmedical.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Droit du patient a un controle gratuit avec ce medecin a cette date, et la consultation payee qui l'ouvre. */
public record ControleGratuitResponse(
        boolean eligible,
        Long rendezVousOrigineId,
        LocalDateTime dateOrigine,
        LocalDate dateLimite,
        int restants,
        String message
) {
    public static ControleGratuitResponse nonEligible(String message) {
        return new ControleGratuitResponse(false, null, null, null, 0, message);
    }
}

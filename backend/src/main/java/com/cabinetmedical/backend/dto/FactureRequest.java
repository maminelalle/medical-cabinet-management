package com.cabinetmedical.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Facture multi-actes. Elle peut etre rattachee a un rendez-vous ou a un acte programme (une seule facture
 * active par origine) et encaissee immediatement avec {@code paiement}.
 */
public record FactureRequest(
        @NotNull Long patientId,
        @NotNull LocalDate dateFacture,
        @NotEmpty List<@Valid LigneFactureRequest> lignes,
        Long rendezVousId,
        Long acteProgrammeId,
        @Valid PaiementRequest paiement
) {
    public FactureRequest(Long patientId, LocalDate dateFacture, List<LigneFactureRequest> lignes) {
        this(patientId, dateFacture, lignes, null, null, null);
    }
}

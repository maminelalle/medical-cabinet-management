package com.cabinetmedical.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Prise ou modification de rendez-vous. Pour un nouveau patient, l'accueil envoie {@code nouveauPatient}
 * a la place de {@code patientId} : la fiche et le rendez-vous sont crees ensemble.
 */
public record RendezVousRequest(
        Long patientId,
        @Valid PatientRequest nouveauPatient,
        @NotNull Long medecinId,
        @NotNull LocalDateTime dateHeure,
        @Min(5) @Max(480) Integer dureeMinutes,
        @Size(max = 500) String motif,
        /** Consultation payee dont ce rendez-vous est le controle gratuit (proposee par /controle-gratuit). */
        Long rendezVousOrigineId
) {
    public int dureeOuDefaut() { return dureeMinutes == null ? 30 : dureeMinutes; }
}

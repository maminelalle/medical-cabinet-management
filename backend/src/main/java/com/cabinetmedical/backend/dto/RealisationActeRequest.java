package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.ResultatActe;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Compte-rendu d'un acte realise : resultat (reussi, partiel, echec), rapport et date reelle. */
public record RealisationActeRequest(
        @NotNull ResultatActe resultat,
        @NotBlank @Size(max = 10000) String compteRendu,
        LocalDateTime dateRealisation
) {}

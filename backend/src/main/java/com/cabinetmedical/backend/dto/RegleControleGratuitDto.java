package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.ParametresCabinet;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Regle du cabinet : une consultation payee donne droit a {@code nombre} consultation(s) de controle gratuite(s)
 * avec le meme medecin pendant {@code jours} jours.
 */
public record RegleControleGratuitDto(
        @NotNull Boolean actif,
        @NotNull @Min(1) @Max(365) Integer jours,
        @NotNull @Min(1) @Max(10) Integer nombre
) {
    public static RegleControleGratuitDto from(ParametresCabinet parametres) {
        return new RegleControleGratuitDto(parametres.isControleGratuitActif(), parametres.getControleGratuitJours(),
                parametres.getControleGratuitNombre());
    }
}

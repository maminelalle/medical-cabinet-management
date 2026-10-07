package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.CatalogueActe;

import java.math.BigDecimal;

/** {@code utilise} : deja present sur une facture (le tarif peut alors etre desactive, pas supprime). */
public record CatalogueActeResponse(Long id, String libelle, String type, BigDecimal montantDefaut, boolean actif,
                                    String specialite, String description, boolean utilise) {
    public static CatalogueActeResponse from(CatalogueActe acte) {
        return from(acte, false);
    }

    public static CatalogueActeResponse from(CatalogueActe acte, boolean utilise) {
        return new CatalogueActeResponse(acte.getId(), acte.getLibelle(), acte.getType(), acte.getMontantDefaut(),
                acte.isActif(), acte.getSpecialite(), acte.getDescription(), utilise);
    }
}

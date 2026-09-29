package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.CatalogueActe;

import java.math.BigDecimal;

public record CatalogueActeResponse(Long id, String libelle, String type, BigDecimal montantDefaut, boolean actif) {
    public static CatalogueActeResponse from(CatalogueActe acte) {
        return new CatalogueActeResponse(acte.getId(), acte.getLibelle(), acte.getType(), acte.getMontantDefaut(), acte.isActif());
    }
}
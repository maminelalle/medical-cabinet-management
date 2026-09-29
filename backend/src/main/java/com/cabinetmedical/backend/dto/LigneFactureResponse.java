package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.LigneFacture;

import java.math.BigDecimal;

public record LigneFactureResponse(Long id, Long catalogueActeId, String libelle, String typeActe, BigDecimal montant) {
    public static LigneFactureResponse from(LigneFacture ligne) {
        return new LigneFactureResponse(ligne.getId(), ligne.getCatalogueActe() == null ? null : ligne.getCatalogueActe().getId(),
                ligne.getLibelle(), ligne.getTypeActe(), ligne.getMontant());
    }
}
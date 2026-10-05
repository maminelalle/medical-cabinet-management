package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Medicament;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicamentResponse(Long id, String nom, String dosage, String forme, Integer stockActuel,
                                 Integer seuilAlerte, BigDecimal prixAchat, BigDecimal prixVente,
                                 String fournisseur, LocalDate dateExpiration, boolean actif) {
    public static MedicamentResponse from(Medicament item) {
        return new MedicamentResponse(item.getId(), item.getNom(), item.getDosage(), item.getForme(),
                item.getStockActuel(), item.getSeuilAlerte(), item.getPrixAchat(), item.getPrixVente(),
                item.getFournisseur(), item.getDateExpiration(), item.isActif());
    }
}
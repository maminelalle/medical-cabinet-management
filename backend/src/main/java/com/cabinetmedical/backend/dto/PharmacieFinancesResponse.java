package com.cabinetmedical.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Mouvements financiers de la pharmacie sur une periode : ventes, achats, marge et encaissements. */
public record PharmacieFinancesResponse(
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal chiffreVentes,
        BigDecimal coutAchats,
        BigDecimal margeBrute,
        BigDecimal encaisse,
        BigDecimal resteAEncaisser,
        long nombreVentes,
        BigDecimal valeurStockAchat,
        BigDecimal valeurStockVente,
        List<MontantParLibelle> encaissementsParMoyen,
        List<MontantParLibelle> meilleuresVentes,
        List<MouvementStockResponse> mouvements
) {
    public record MontantParLibelle(String libelle, long nombre, BigDecimal montant) {}
}

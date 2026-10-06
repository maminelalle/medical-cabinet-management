package com.cabinetmedical.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Fiche complete d'un medicament : informations, indicateurs de ventes et d'achats, historique des mouvements. */
public record MedicamentDetailResponse(
        MedicamentResponse medicament,
        int quantiteVendue,
        BigDecimal chiffreVentes,
        int quantiteAchetee,
        BigDecimal coutAchats,
        BigDecimal margeBrute,
        BigDecimal valeurStockAchat,
        BigDecimal valeurStockVente,
        Instant derniereVente,
        Instant dernierAchat,
        List<MouvementStockResponse> mouvements
) {}

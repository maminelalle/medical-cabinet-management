package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Paiement;

import java.math.BigDecimal;
import java.time.Instant;

public record PaiementResponse(Long id, BigDecimal montant, Instant datePaiement, String moyenPaiement) {
    public static PaiementResponse from(Paiement paiement) {
        return new PaiementResponse(paiement.getId(), paiement.getMontant(), paiement.getDatePaiement(), paiement.getMoyenPaiement());
    }
}
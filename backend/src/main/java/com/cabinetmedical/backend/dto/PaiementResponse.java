package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Paiement;
import com.cabinetmedical.backend.entity.Utilisateur;

import java.math.BigDecimal;
import java.time.Instant;

public record PaiementResponse(Long id, BigDecimal montant, Instant datePaiement, String moyenPaiement,
                               String reference, String enregistrePar) {
    public static PaiementResponse from(Paiement paiement) {
        return new PaiementResponse(paiement.getId(), paiement.getMontant(), paiement.getDatePaiement(),
                paiement.getMoyenPaiement(), paiement.getReference(), nom(paiement.getEnregistrePar()));
    }

    static String nom(Utilisateur utilisateur) {
        if (utilisateur == null) return null;
        String nomComplet = ((utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom() + " ")
                + (utilisateur.getNom() == null ? "" : utilisateur.getNom())).trim();
        return nomComplet.isEmpty() ? utilisateur.getEmail() : nomComplet;
    }
}

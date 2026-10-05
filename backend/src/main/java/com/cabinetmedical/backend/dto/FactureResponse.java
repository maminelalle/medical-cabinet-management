package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.StatutFacture;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FactureResponse(Long id, Long patientId, String patientNom, String patientPrenom,
                              LocalDate dateFacture, BigDecimal montantTotal, BigDecimal montantPaye,
                              BigDecimal resteAPayer, StatutFacture statut,
                              List<LigneFactureResponse> lignes, List<PaiementResponse> paiements,
                              Instant createdAt, String motifAnnulation, Instant dateAnnulation) {
    public static FactureResponse from(Facture facture, BigDecimal montantPaye,
                                       List<LigneFactureResponse> lignes, List<PaiementResponse> paiements) {
        // Une facture annulee ne laisse plus rien a payer.
        BigDecimal reste = facture.getStatut() == StatutFacture.ANNULEE
                ? BigDecimal.ZERO : facture.getMontantTotal().subtract(montantPaye);
        return new FactureResponse(facture.getId(), facture.getPatient().getId(), facture.getPatient().getNom(),
                facture.getPatient().getPrenom(), facture.getDateFacture(), facture.getMontantTotal(), montantPaye,
                reste, facture.getStatut(), lignes, paiements, facture.getCreatedAt(),
                facture.getMotifAnnulation(), facture.getDateAnnulation());
    }
}

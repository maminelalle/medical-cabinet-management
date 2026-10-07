package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.StatutFacture;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facture et son origine : CONSULTATION (rendez-vous), ACTE (acte programme), SOIN, PHARMACIE (dispensation) ou LIBRE.
 */
public record FactureResponse(Long id, Long patientId, String patientNom, String patientPrenom,
                              LocalDate dateFacture, BigDecimal montantTotal, BigDecimal montantPaye,
                              BigDecimal resteAPayer, StatutFacture statut,
                              List<LigneFactureResponse> lignes, List<PaiementResponse> paiements,
                              Instant createdAt, String creePar, String motifAnnulation, Instant dateAnnulation,
                              String origine, Long rendezVousId, LocalDateTime rendezVousDateHeure,
                              String rendezVousMedecin, Long acteProgrammeId, String acteIntitule, Long dispensationId,
                              Long soinId, String soinIntitule) {
    public static FactureResponse from(Facture facture, BigDecimal montantPaye,
                                       List<LigneFactureResponse> lignes, List<PaiementResponse> paiements,
                                       Long dispensationId) {
        // Une facture annulee ne laisse plus rien a payer.
        BigDecimal reste = facture.getStatut() == StatutFacture.ANNULEE
                ? BigDecimal.ZERO : facture.getMontantTotal().subtract(montantPaye);
        var rendezVous = facture.getRendezVous();
        var acte = facture.getActeProgramme();
        var soin = facture.getSoin();
        // Les ventes pharmacie anterieures au lien dispensation -> facture se reconnaissent a leurs lignes.
        boolean lignesPharmacie = !lignes.isEmpty() && lignes.stream().allMatch(ligne -> "PHARMACIE".equals(ligne.typeActe()));
        String origine = rendezVous != null ? "CONSULTATION" : acte != null ? "ACTE" : soin != null ? "SOIN"
                : dispensationId != null || lignesPharmacie ? "PHARMACIE" : "LIBRE";
        return new FactureResponse(facture.getId(), facture.getPatient().getId(), facture.getPatient().getNom(),
                facture.getPatient().getPrenom(), facture.getDateFacture(), facture.getMontantTotal(), montantPaye,
                reste, facture.getStatut(), lignes, paiements, facture.getCreatedAt(),
                PaiementResponse.nom(facture.getCreatedBy()), facture.getMotifAnnulation(), facture.getDateAnnulation(),
                origine,
                rendezVous == null ? null : rendezVous.getId(),
                rendezVous == null ? null : rendezVous.getDateHeure(),
                rendezVous == null ? null : "Dr. " + rendezVous.getMedecin().getPrenom() + " " + rendezVous.getMedecin().getNom(),
                acte == null ? null : acte.getId(),
                acte == null ? null : acte.getIntitule(),
                dispensationId,
                soin == null ? null : soin.getId(),
                soin == null ? null : soin.getIntitule());
    }
}

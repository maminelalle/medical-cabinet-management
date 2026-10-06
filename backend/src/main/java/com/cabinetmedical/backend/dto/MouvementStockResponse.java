package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.MouvementStock;
import com.cabinetmedical.backend.entity.TypeMouvement;

import java.math.BigDecimal;
import java.time.Instant;

public record MouvementStockResponse(Long id, Long medicamentId, String medicament, TypeMouvement type, Integer quantite,
                                     Integer stockApres, BigDecimal prixUnitaire, BigDecimal montant, String fournisseur,
                                     String reference, String commentaire, Long dispensationId, Long factureId,
                                     String patient, String utilisateur, Instant dateMouvement) {
    public static MouvementStockResponse from(MouvementStock mouvement) {
        var medicament = mouvement.getMedicament();
        var dispensation = mouvement.getDispensation();
        return new MouvementStockResponse(mouvement.getId(), medicament.getId(),
                medicament.getNom() + (medicament.getDosage() == null ? "" : " " + medicament.getDosage()),
                mouvement.getType(), mouvement.getQuantite(), mouvement.getStockApres(), mouvement.getPrixUnitaire(),
                mouvement.getMontant(), mouvement.getFournisseur(), mouvement.getReference(), mouvement.getCommentaire(),
                dispensation == null ? null : dispensation.getId(),
                dispensation == null || dispensation.getFacture() == null ? null : dispensation.getFacture().getId(),
                dispensation == null ? null : dispensation.getPatient().getPrenom() + " " + dispensation.getPatient().getNom(),
                PaiementResponse.nom(mouvement.getUtilisateur()), mouvement.getDateMouvement());
    }
}

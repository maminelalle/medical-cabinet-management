package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.Soin;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutSoin;
import com.cabinetmedical.backend.entity.TypeSoin;

import java.time.Instant;
import java.time.LocalDateTime;

public record SoinResponse(
        Long id,
        Long patientId,
        String patientNom,
        String patientPrenom,
        String patientTelephone,
        TypeSoin type,
        String intitule,
        String produit,
        Long prescriptionId,
        String prescripteur,
        LocalDateTime dateHeure,
        StatutSoin statut,
        String observations,
        Instant debut,
        Instant fin,
        String realisePar,
        Long factureId,
        StatutFacture factureStatut,
        Instant createdAt
) {
    /** {@code facture} = la facture active (non annulee) du soin, s'il en existe une. */
    public static SoinResponse from(Soin soin, Facture facture) {
        var patient = soin.getPatient();
        var prescription = soin.getPrescription();
        String prescripteur = prescription != null
                ? "Dr. " + prescription.getConsultation().getRendezVous().getMedecin().getPrenom() + " "
                  + prescription.getConsultation().getRendezVous().getMedecin().getNom()
                : soin.getPrescripteurExterne();
        return new SoinResponse(soin.getId(), patient.getId(), patient.getNom(), patient.getPrenom(), patient.getTelephone(),
                soin.getType(), soin.getIntitule(), soin.getProduit(),
                prescription == null ? null : prescription.getId(), prescripteur,
                soin.getDateHeure(), soin.getStatut(), soin.getObservations(), soin.getDebut(), soin.getFin(),
                PaiementResponse.nom(soin.getRealisePar()),
                facture == null ? null : facture.getId(), facture == null ? null : facture.getStatut(),
                soin.getCreatedAt());
    }
}

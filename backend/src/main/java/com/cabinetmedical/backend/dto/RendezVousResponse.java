package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;

import java.time.Instant;
import java.time.LocalDateTime;

public record RendezVousResponse(
        Long id,
        Long patientId,
        String patientNom,
        String patientPrenom,
        String patientTelephone,
        Long medecinId,
        String medecinNom,
        String medecinPrenom,
        LocalDateTime dateHeure,
        Integer dureeMinutes,
        String motif,
        Integer numeroFile,
        StatutRendezVous statut,
        Instant debutConsultation,
        Instant finConsultation,
        Long rendezVousOrigineId,
        boolean controleGratuit,
        Long factureId,
        StatutFacture factureStatut,
        Instant createdAt
) {
    public static RendezVousResponse from(RendezVous rendezVous) {
        return from(rendezVous, null, rendezVous.getRendezVousOrigine() != null);
    }

    /** {@code facture} = la facture active (non annulee) ; {@code controleGratuit} = controle couvert par la consultation payee. */
    public static RendezVousResponse from(RendezVous rendezVous, Facture facture, boolean controleGratuit) {
        return new RendezVousResponse(
                rendezVous.getId(),
                rendezVous.getPatient().getId(),
                rendezVous.getPatient().getNom(),
                rendezVous.getPatient().getPrenom(),
                rendezVous.getPatient().getTelephone(),
                rendezVous.getMedecin().getId(),
                rendezVous.getMedecin().getNom(),
                rendezVous.getMedecin().getPrenom(),
                rendezVous.getDateHeure(),
                rendezVous.getDureeMinutes(),
                rendezVous.getMotif(),
                rendezVous.getNumeroFile(),
                rendezVous.getStatut(),
                rendezVous.getDebutConsultation(),
                rendezVous.getFinConsultation(),
                rendezVous.getRendezVousOrigine() == null ? null : rendezVous.getRendezVousOrigine().getId(),
                controleGratuit,
                facture == null ? null : facture.getId(),
                facture == null ? null : facture.getStatut(),
                rendezVous.getCreatedAt()
        );
    }
}

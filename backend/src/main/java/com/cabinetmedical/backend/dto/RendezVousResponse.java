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
        Long factureId,
        StatutFacture factureStatut,
        Instant createdAt
) {
    public static RendezVousResponse from(RendezVous rendezVous) {
        return from(rendezVous, null);
    }

    /** {@code facture} = la facture active (non annulee) du rendez-vous, s'il en existe une. */
    public static RendezVousResponse from(RendezVous rendezVous, Facture facture) {
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
                facture == null ? null : facture.getId(),
                facture == null ? null : facture.getStatut(),
                rendezVous.getCreatedAt()
        );
    }
}

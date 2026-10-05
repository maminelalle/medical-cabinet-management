package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutRendezVous;

import java.time.Instant;
import java.time.LocalDateTime;

public record RendezVousResponse(
        Long id,
        Long patientId,
        String patientNom,
        String patientPrenom,
        Long medecinId,
        String medecinNom,
        String medecinPrenom,
        LocalDateTime dateHeure,
        String motif,
        Integer numeroFile,
        StatutRendezVous statut,
        Instant createdAt
) {
    public static RendezVousResponse from(RendezVous rendezVous) {
        return new RendezVousResponse(
                rendezVous.getId(),
                rendezVous.getPatient().getId(),
                rendezVous.getPatient().getNom(),
                rendezVous.getPatient().getPrenom(),
                rendezVous.getMedecin().getId(),
                rendezVous.getMedecin().getNom(),
                rendezVous.getMedecin().getPrenom(),
                rendezVous.getDateHeure(),
                rendezVous.getMotif(),
                rendezVous.getNumeroFile(),
                rendezVous.getStatut(),
                rendezVous.getCreatedAt()
        );
    }
}
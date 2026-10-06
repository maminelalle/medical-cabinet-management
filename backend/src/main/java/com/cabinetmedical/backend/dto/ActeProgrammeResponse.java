package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.ActeProgramme;
import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.ResultatActe;
import com.cabinetmedical.backend.entity.StatutActe;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.TypeActe;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActeProgrammeResponse(
        Long id,
        Long patientId,
        String patientNom,
        String patientPrenom,
        LocalDate patientDateNaissance,
        Long medecinId,
        String medecinNom,
        String medecinPrenom,
        String specialite,
        Long consultationId,
        TypeActe type,
        String intitule,
        String details,
        LocalDateTime dateHeure,
        Integer dureeMinutes,
        String lieu,
        StatutActe statut,
        ResultatActe resultat,
        String compteRendu,
        boolean compteRenduMasque,
        LocalDateTime dateRealisation,
        String motifAnnulation,
        Long factureId,
        StatutFacture factureStatut,
        Instant createdAt
) {
    /** {@code masquerCompteRendu} : le rapport medical n'est pas montre a l'accueil ni a l'administration. */
    public static ActeProgrammeResponse from(ActeProgramme acte, Facture facture, boolean masquerCompteRendu) {
        var patient = acte.getPatient();
        var medecin = acte.getMedecin();
        return new ActeProgrammeResponse(acte.getId(), patient.getId(), patient.getNom(), patient.getPrenom(),
                patient.getDateNaissance(), medecin.getId(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite(),
                acte.getConsultation() == null ? null : acte.getConsultation().getId(), acte.getType(), acte.getIntitule(),
                acte.getDetails(), acte.getDateHeure(), acte.getDureeMinutes(), acte.getLieu(), acte.getStatut(),
                acte.getResultat(), masquerCompteRendu ? null : acte.getCompteRendu(),
                masquerCompteRendu && acte.getCompteRendu() != null, acte.getDateRealisation(), acte.getMotifAnnulation(),
                facture == null ? null : facture.getId(), facture == null ? null : facture.getStatut(), acte.getCreatedAt());
    }
}

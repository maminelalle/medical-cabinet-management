package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dossier patient importe depuis un fichier JSON exporte par l'application.
 * Le format reprend celui de {@link DossierPatientResponse} : les champs techniques (identifiants...) sont ignores.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DossierImportRequest(
        @NotNull @Valid PatientImport patient,
        List<@Valid ConsultationImport> consultations,
        List<@Valid RendezVousImport> rendezVous,
        List<@Valid FactureImport> factures
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatientImport(@NotBlank String nom, @NotBlank String prenom, @NotNull LocalDate dateNaissance,
                                String telephone, @Email String email, String adresse) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsultationImport(@NotNull LocalDateTime dateHeure, String motif, String medecinNom,
                                     String medecinPrenom, String compteRendu, @Valid PrescriptionImport prescription) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrescriptionImport(LocalDate datePrescription, String instructions,
                                     List<@Valid LignePrescriptionImport> lignes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LignePrescriptionImport(@NotBlank String medicament, String posologie, String duree) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RendezVousImport(@NotNull LocalDateTime dateHeure, String motif, String medecinNom,
                                   String medecinPrenom, StatutRendezVous statut) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FactureImport(@NotNull LocalDate dateFacture, StatutFacture statut,
                                List<@Valid LigneFactureImport> lignes, List<@Valid PaiementImport> paiements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LigneFactureImport(@NotBlank String libelle, String typeActe, @NotNull BigDecimal montant) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaiementImport(@NotNull BigDecimal montant, Instant datePaiement, String moyenPaiement) {}
}

package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.TypeSoin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Enregistrement d'un soin. Comme pour un rendez-vous, un patient inconnu peut etre cree en meme temps
 * ({@code nouveauPatient}). {@code dateHeure} absente = maintenant (patient present a l'accueil).
 */
public record SoinRequest(
        Long patientId,
        @Valid PatientRequest nouveauPatient,
        @NotNull TypeSoin type,
        @NotBlank @Size(max = 200) String intitule,
        @Size(max = 255) String produit,
        Long prescriptionId,
        @Size(max = 200) String prescripteurExterne,
        LocalDateTime dateHeure,
        String observations
) {}

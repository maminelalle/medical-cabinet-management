package com.cabinetmedical.backend.dto;

/**
 * Profil de l'utilisateur connecte : identite reelle issue de la base,
 * completee par les informations du medecin lorsque le compte en possede un.
 */
public record ProfilResponse(
        Long id,
        String email,
        String role,
        String nom,
        String prenom,
        String specialite
) {}
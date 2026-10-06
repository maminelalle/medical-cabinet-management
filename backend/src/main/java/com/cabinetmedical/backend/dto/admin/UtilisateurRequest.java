package com.cabinetmedical.backend.dto.admin;

import com.cabinetmedical.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creation ou modification d'un compte. Le mot de passe n'est lu qu'a la creation. */
public record UtilisateurRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @Size(max = 30) String telephone,
        @NotNull Role role,
        @Size(min = 8, max = 100) String motDePasse,
        @Size(max = 150) String specialite,
        @Size(max = 50) String numeroOrdre
) {}

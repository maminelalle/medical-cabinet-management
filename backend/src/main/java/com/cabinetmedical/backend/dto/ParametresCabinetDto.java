package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.ParametresCabinet;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Coordonnees du cabinet (lecture par tous, modification par l'administrateur). */
public record ParametresCabinetDto(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 150) String sousTitre,
        @Size(max = 255) String adresse,
        @Size(max = 50) String telephone,
        @Email @Size(max = 255) String email
) {
    public static ParametresCabinetDto from(ParametresCabinet parametres) {
        return new ParametresCabinetDto(parametres.getNom(), parametres.getSousTitre(), parametres.getAdresse(),
                parametres.getTelephone(), parametres.getEmail());
    }

    /** Ligne de coordonnees imprimee sous le nom du cabinet. */
    public String coordonnees() {
        StringBuilder texte = new StringBuilder(sousTitre == null ? "" : sousTitre);
        for (String partie : new String[] {adresse, telephone == null ? null : "Tél. " + telephone, email}) {
            if (partie != null && !partie.isBlank()) texte.append(texte.isEmpty() ? "" : " · ").append(partie);
        }
        return texte.toString();
    }
}

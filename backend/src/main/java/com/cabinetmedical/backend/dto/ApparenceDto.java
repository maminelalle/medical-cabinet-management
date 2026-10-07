package com.cabinetmedical.backend.dto;

import com.cabinetmedical.backend.entity.ParametresCabinet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Apparence de l'interface reglee par l'administrateur : nom affiche, sous-titre, logo et couleurs.
 * Lisible sans connexion (page de connexion), modifiable par l'administrateur.
 */
public record ApparenceDto(
        @NotBlank @Size(max = 80) String nomInterface,
        @Size(max = 150) String sousTitre,
        @NotBlank @Pattern(regexp = COULEUR, message = "doit être une couleur hexadécimale (#RRGGBB)") String couleurPrincipale,
        @NotBlank @Pattern(regexp = COULEUR, message = "doit être une couleur hexadécimale (#RRGGBB)") String couleurAccent,
        @NotBlank @Pattern(regexp = COULEUR, message = "doit être une couleur hexadécimale (#RRGGBB)") String couleurBouton,
        @Size(max = 400_000, message = "le logo ne doit pas dépasser 300 Ko")
        @Pattern(regexp = "^data:image/(png|jpeg|webp|svg\\+xml);base64,[A-Za-z0-9+/=]+$",
                message = "doit être une image PNG, JPEG, WEBP ou SVG") String logo
) {
    public static final String COULEUR = "^#[0-9a-fA-F]{6}$";

    public static ApparenceDto from(ParametresCabinet parametres) {
        return new ApparenceDto(parametres.getNomInterface(), parametres.getSousTitre(), parametres.getCouleurPrincipale(),
                parametres.getCouleurAccent(), parametres.getCouleurBouton(), parametres.getLogo());
    }

    public static ApparenceDto parDefaut() {
        return from(new ParametresCabinet());
    }
}

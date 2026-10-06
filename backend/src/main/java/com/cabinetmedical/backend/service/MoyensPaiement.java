package com.cabinetmedical.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Moyens de paiement acceptes par la caisse et la pharmacie. */
public final class MoyensPaiement {
    public static final String ESPECES = "ESPECES";
    public static final List<String> TOUS = List.of(ESPECES, "BANKILY", "MASRVI", "SEDAD", "CARTE", "VIREMENT", "CHEQUE");

    private MoyensPaiement() {}

    /** Normalise le moyen et la reference ; refuse un moyen inconnu ou une reference manquante hors especes. */
    public static String[] valider(String moyen, String reference) {
        String moyenNormalise = moyen == null ? "" : moyen.trim().toUpperCase();
        if (!TOUS.contains(moyenNormalise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Moyen de paiement inconnu. Valeurs acceptées : " + String.join(", ", TOUS));
        }
        String referenceNormalisee = reference == null || reference.isBlank() ? null : reference.trim();
        if (!ESPECES.equals(moyenNormalise) && referenceNormalisee == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La référence de la transaction est obligatoire pour un paiement par " + moyenNormalise);
        }
        return new String[] {moyenNormalise, referenceNormalisee};
    }
}

package com.cabinetmedical.backend.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Format unique des erreurs renvoyees par l'API.
 * {@code champs} detaille les erreurs de validation (champ -> message), vide sinon.
 */
public record ErreurResponse(
        Instant horodatage,
        int statut,
        String erreur,
        String message,
        String chemin,
        Map<String, String> champs
) {}

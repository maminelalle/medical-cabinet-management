package com.cabinetmedical.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduit toutes les exceptions de l'API en une {@link ErreurResponse} homogene.
 * Les journaux ne contiennent que le type d'erreur et le chemin, jamais de donnee medicale.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErreurResponse> statut(ResponseStatusException exception, HttpServletRequest requete) {
        return reponse(exception.getStatusCode(), exception.getReason(), requete, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest requete) {
        Map<String, String> champs = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erreur -> champs.putIfAbsent(erreur.getField(), erreur.getDefaultMessage()));
        return reponse(HttpStatus.BAD_REQUEST, "Certains champs sont invalides", requete, champs);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErreurResponse> requeteIllisible(Exception exception, HttpServletRequest requete) {
        return reponse(HttpStatus.BAD_REQUEST, "La requête est mal formée ou contient une valeur invalide", requete, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErreurResponse> authentification(AuthenticationException exception, HttpServletRequest requete) {
        return reponse(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect", requete, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErreurResponse> accesRefuse(AccessDeniedException exception, HttpServletRequest requete) {
        return reponse(HttpStatus.FORBIDDEN, "Votre rôle ne permet pas cette action", requete, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErreurResponse> integrite(DataIntegrityViolationException exception, HttpServletRequest requete) {
        log.warn("Contrainte d'integrite violee sur {}", requete.getRequestURI());
        return reponse(HttpStatus.CONFLICT, "Opération impossible : elle entre en conflit avec des données existantes", requete, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurResponse> autre(Exception exception, HttpServletRequest requete) {
        // Exceptions standard de Spring MVC (route inconnue, methode non supportee...) : on garde leur statut.
        if (exception instanceof ErrorResponse erreurSpring) {
            return reponse(erreurSpring.getStatusCode(), erreurSpring.getBody().getDetail(), requete, Map.of());
        }
        log.error("Erreur inattendue sur {} : {}", requete.getRequestURI(), exception.getClass().getName());
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", requete, Map.of());
    }

    private ResponseEntity<ErreurResponse> reponse(HttpStatusCode statut, String message, HttpServletRequest requete,
                                                   Map<String, String> champs) {
        HttpStatus connu = HttpStatus.resolve(statut.value());
        String libelle = connu != null ? connu.getReasonPhrase() : String.valueOf(statut.value());
        return ResponseEntity.status(statut).body(new ErreurResponse(Instant.now(), statut.value(), libelle,
                message != null ? message : libelle, requete.getRequestURI(), champs));
    }
}

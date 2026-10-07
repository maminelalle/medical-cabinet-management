package com.cabinetmedical.backend.temps;

import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.security.JwtAuthenticationFilter;
import com.cabinetmedical.backend.service.UtilisateurConnecte;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

/**
 * Flux d'evenements temps reel. Le navigateur (EventSource) ne peut pas envoyer d'en-tete :
 * le jeton est transmis dans le parametre "jeton", accepte uniquement sur ce chemin.
 */
@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor
public class EvenementsController {
    private final TempsReelService tempsReelService;
    private final UtilisateurConnecte utilisateurConnecte;
    private final MedecinRepository medecinRepository;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter abonner(@AuthenticationPrincipal UserDetails connecte, HttpServletRequest requete) {
        Utilisateur utilisateur = utilisateurConnecte.utilisateur(connecte);
        Object sessionId = requete.getAttribute(JwtAuthenticationFilter.ATTRIBUT_SESSION);
        return tempsReelService.abonner(utilisateur.getId(), sessionId == null ? null : sessionId.toString(), nomAffiche(utilisateur));
    }

    private String nomAffiche(Utilisateur utilisateur) {
        Medecin medecin = medecinRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        if (medecin != null) return "Dr. " + medecin.getPrenom() + " " + medecin.getNom();
        String nom = (Objects.toString(utilisateur.getPrenom(), "") + " " + Objects.toString(utilisateur.getNom(), "")).trim();
        return nom.isEmpty() ? utilisateur.getEmail() : nom;
    }
}

package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.LoginRequest;
import com.cabinetmedical.backend.dto.LoginResponse;
import com.cabinetmedical.backend.entity.SessionUtilisateur;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import com.cabinetmedical.backend.security.JwtAuthenticationFilter;
import com.cabinetmedical.backend.security.JwtService;
import com.cabinetmedical.backend.security.SessionService;
import com.cabinetmedical.backend.service.JournalService;
import com.cabinetmedical.backend.temps.TempsReelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final JournalService journalService;
    private final UtilisateurRepository utilisateurRepository;
    private final TempsReelService tempsReelService;

    /** Connexion : ouvre une session (appareil, adresse IP) et renvoie un jeton qui la reference. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest requete) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.motDePasse()));
        } catch (AuthenticationException echec) {
            journalService.enregistrer(request.email(), "Échec de connexion", echec.getClass().getSimpleName(), requete, 401);
            throw echec;
        }
        var user = (UserDetails) authentication.getPrincipal();
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(user.getUsername()).orElseThrow();
        utilisateur.setDerniereConnexion(Instant.now());
        utilisateurRepository.save(utilisateur);
        SessionUtilisateur session = sessionService.ouvrir(utilisateur, requete);
        journalService.enregistrer(user.getUsername(), "Connexion",
                session.getAppareil() + " · " + session.getNavigateur() + " · " + session.getSysteme(), requete, 200);
        String role = user.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return new LoginResponse(jwtService.generateToken(user, session.getJetonId()), user.getUsername(), role);
    }

    /** Deconnexion : la session est fermee, son jeton ne sera plus accepte. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UserDetails connecte, HttpServletRequest requete) {
        Object sessionId = requete.getAttribute(JwtAuthenticationFilter.ATTRIBUT_SESSION);
        if (sessionId != null) {
            sessionService.fermer(sessionId.toString(), SessionUtilisateur.FIN_DECONNEXION);
            tempsReelService.fermerSession(sessionId.toString());
        }
        if (connecte != null) journalService.enregistrer(connecte.getUsername(), "Déconnexion", null, requete, 204);
    }

    /** Signal de presence envoye regulierement par l'interface (met a jour la derniere activite). */
    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ping() {
        // La mise a jour de l'activite est faite par le filtre JWT.
    }
}

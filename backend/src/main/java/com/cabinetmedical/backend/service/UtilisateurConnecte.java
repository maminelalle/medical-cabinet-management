package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Acces au compte de l'utilisateur connecte et a son profil medecin. */
@Component
@RequiredArgsConstructor
public class UtilisateurConnecte {
    private final UtilisateurRepository utilisateurRepository;
    private final MedecinRepository medecinRepository;

    public Utilisateur utilisateur(UserDetails connecte) {
        return utilisateurRepository.findByEmailIgnoreCase(connecte.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    /** Profil medecin du connecte ; refuse si l'utilisateur n'est pas medecin. */
    public Medecin medecin(UserDetails connecte) {
        return medecinRepository.findByUtilisateurId(utilisateur(connecte).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Profil médecin introuvable"));
    }

    /** Identifiant medecin du connecte, ou null s'il n'a pas le role MEDECIN. */
    public Long medecinIdOuNull(UserDetails connecte) {
        return aLeRole(connecte, "MEDECIN") ? medecin(connecte).getId() : null;
    }

    public static boolean aLeRole(UserDetails connecte, String role) {
        return connecte.getAuthorities().stream().anyMatch(autorite -> autorite.getAuthority().equals("ROLE_" + role));
    }
}

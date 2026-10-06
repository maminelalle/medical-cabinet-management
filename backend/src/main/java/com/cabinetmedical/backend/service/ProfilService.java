package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ProfilResponse;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfilService {
    private final UtilisateurRepository utilisateurRepository;
    private final MedecinRepository medecinRepository;

    @Transactional(readOnly = true)
    public ProfilResponse profil(UserDetails utilisateurConnecte) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        return medecinRepository.findByUtilisateurId(utilisateur.getId())
                .map(medecin -> new ProfilResponse(utilisateur.getId(), utilisateur.getEmail(),
                        utilisateur.getRole().name(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite()))
                .orElseGet(() -> new ProfilResponse(utilisateur.getId(), utilisateur.getEmail(),
                        utilisateur.getRole().name(), utilisateur.getNom(), utilisateur.getPrenom(), null));
    }
}
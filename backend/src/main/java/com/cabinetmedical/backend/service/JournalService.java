package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.entity.JournalActivite;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.JournalActiviteRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import com.cabinetmedical.backend.security.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ecriture du journal d'activite (qui, quoi, quand, d'ou). Aucune donnee medicale n'y est ecrite. */
@Service
@RequiredArgsConstructor
public class JournalService {
    private final JournalActiviteRepository repository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String email, String action, String description, HttpServletRequest requete, Integer statutHttp) {
        Utilisateur utilisateur = email == null ? null : utilisateurRepository.findByEmailIgnoreCase(email).orElse(null);
        JournalActivite entree = new JournalActivite();
        entree.setUtilisateur(utilisateur);
        entree.setEmail(email);
        entree.setRole(utilisateur == null ? null : utilisateur.getRole().name());
        entree.setAction(couper(action, 60));
        entree.setDescription(couper(description, 500));
        if (requete != null) {
            entree.setMethode(requete.getMethod());
            entree.setChemin(couper(requete.getRequestURI(), 255));
            entree.setAdresseIp(SessionService.adresseIp(requete));
        }
        entree.setStatutHttp(statutHttp);
        repository.save(entree);
    }

    private static String couper(String texte, int longueur) {
        return texte == null ? null : texte.substring(0, Math.min(longueur, texte.length()));
    }
}

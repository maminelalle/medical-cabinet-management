package com.cabinetmedical.backend.security;

import com.cabinetmedical.backend.entity.SessionUtilisateur;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.SessionUtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sessions de connexion : ouverture a la connexion, suivi de l'activite, fermeture (deconnexion) et revocation.
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    /** Au-dela de ce delai sans requete, l'utilisateur n'est plus considere en ligne. */
    public static final Duration EN_LIGNE = Duration.ofMinutes(5);
    private static final Duration RAFRAICHISSEMENT = Duration.ofSeconds(60);

    private final SessionUtilisateurRepository repository;

    @Transactional
    public SessionUtilisateur ouvrir(Utilisateur utilisateur, HttpServletRequest requete) {
        String userAgent = requete.getHeader("User-Agent");
        SessionUtilisateur session = new SessionUtilisateur();
        session.setUtilisateur(utilisateur);
        session.setJetonId(UUID.randomUUID().toString());
        session.setAdresseIp(adresseIp(requete));
        session.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(500, userAgent.length())));
        session.setAppareil(appareil(userAgent));
        session.setNavigateur(navigateur(userAgent));
        session.setSysteme(systeme(userAgent));
        return repository.save(session);
    }

    /** Session encore ouverte ? Met a jour la derniere activite au plus une fois par minute. */
    @Transactional
    public boolean valider(String jetonId) {
        SessionUtilisateur session = repository.findByJetonId(jetonId).orElse(null);
        if (session == null || session.getDateFin() != null) return false;
        Instant maintenant = Instant.now();
        if (session.getDerniereActivite().plus(RAFRAICHISSEMENT).isBefore(maintenant)) {
            session.setDerniereActivite(maintenant);
            repository.save(session);
        }
        return true;
    }

    @Transactional
    public void fermer(String jetonId, String motif) {
        repository.findByJetonId(jetonId).filter(session -> session.getDateFin() == null).ifPresent(session -> {
            session.setDateFin(Instant.now());
            session.setMotifFin(motif);
            repository.save(session);
        });
    }

    @Transactional
    public int fermerToutes(Long utilisateurId, String motif) {
        var sessions = repository.findByUtilisateurIdAndDateFinIsNull(utilisateurId);
        sessions.forEach(session -> {
            session.setDateFin(Instant.now());
            session.setMotifFin(motif);
        });
        repository.saveAll(sessions);
        return sessions.size();
    }

    public static String adresseIp(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        String ip = transmise != null && !transmise.isBlank() ? transmise.split(",")[0].trim() : requete.getRemoteAddr();
        return ip == null ? null : ip.substring(0, Math.min(64, ip.length()));
    }

    static String appareil(String ua) {
        if (ua == null) return "Inconnu";
        String texte = ua.toLowerCase();
        if (texte.contains("ipad") || texte.contains("tablet")) return "Tablette";
        if (texte.contains("mobi") || texte.contains("iphone") || texte.contains("android")) return "Téléphone";
        if (texte.contains("postman") || texte.contains("curl") || texte.contains("java")) return "Outil / API";
        return "Ordinateur";
    }

    static String navigateur(String ua) {
        if (ua == null) return "Inconnu";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Chrome/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        if (ua.contains("PostmanRuntime")) return "Postman";
        return "Autre";
    }

    static String systeme(String ua) {
        if (ua == null) return "Inconnu";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Autre";
    }
}

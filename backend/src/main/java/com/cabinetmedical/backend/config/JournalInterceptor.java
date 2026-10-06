package com.cabinetmedical.backend.config;

import com.cabinetmedical.backend.service.JournalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Journalise chaque action reussie qui modifie des donnees (POST, PUT, PATCH, DELETE) et chaque export PDF.
 * Seuls le libelle de l'action et les identifiants techniques sont conserves.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalInterceptor implements HandlerInterceptor {
    private static final List<Regle> REGLES = List.of(
            new Regle("POST", "/api/patients/import", "Import d'un dossier patient"),
            new Regle("GET", "/api/patients/(\\d+)/dossier\\.pdf", "Export PDF d'un dossier patient"),
            new Regle("POST", "/api/patients", "Création d'un patient"),
            new Regle("PUT", "/api/patients/(\\d+)", "Modification d'un patient"),
            new Regle("DELETE", "/api/patients/(\\d+)", "Suppression d'un patient"),
            new Regle("POST", "/api/rendezvous", "Prise de rendez-vous"),
            new Regle("PUT", "/api/rendezvous/(\\d+)", "Modification d'un rendez-vous"),
            new Regle("PATCH", "/api/rendezvous/(\\d+)/statut", "Changement de statut d'un rendez-vous"),
            new Regle("DELETE", "/api/rendezvous/(\\d+)", "Suppression d'un rendez-vous"),
            new Regle("POST", "/api/rendezvous/(\\d+)/consultation", "Compte-rendu de consultation"),
            new Regle("POST", "/api/consultations/(\\d+)/prescriptions", "Rédaction d'une ordonnance"),
            new Regle("POST", "/api/actes", "Programmation d'un acte"),
            new Regle("PUT", "/api/actes/(\\d+)", "Modification d'un acte programmé"),
            new Regle("POST", "/api/actes/(\\d+)/realisation", "Compte-rendu d'un acte réalisé"),
            new Regle("POST", "/api/actes/(\\d+)/annulation", "Annulation d'un acte programmé"),
            new Regle("POST", "/api/factures", "Création d'une facture"),
            new Regle("POST", "/api/factures/(\\d+)/paiements", "Encaissement d'un paiement"),
            new Regle("POST", "/api/factures/(\\d+)/annulation", "Annulation d'une facture"),
            new Regle("POST", "/api/actes-catalogue", "Ajout d'un acte au catalogue"),
            new Regle("POST", "/api/pharmacie/medicaments", "Ajout d'un médicament"),
            new Regle("PUT", "/api/pharmacie/medicaments/(\\d+)", "Modification d'un médicament"),
            new Regle("POST", "/api/pharmacie/medicaments/import", "Import de médicaments"),
            new Regle("PATCH", "/api/pharmacie/medicaments/(\\d+)/stock", "Correction de stock"),
            new Regle("POST", "/api/pharmacie/medicaments/(\\d+)/approvisionnements", "Approvisionnement en stock"),
            new Regle("GET", "/api/pharmacie/medicaments/inventaire\\.pdf", "Export PDF de l'inventaire"),
            new Regle("POST", "/api/pharmacie/dispensations", "Vente d'une ordonnance (pharmacie)"),
            new Regle("POST", "/api/admin/utilisateurs", "Création d'un utilisateur"),
            new Regle("PUT", "/api/admin/utilisateurs/(\\d+)", "Modification d'un utilisateur"),
            new Regle("PATCH", "/api/admin/utilisateurs/(\\d+)/statut", "Activation / désactivation d'un compte"),
            new Regle("POST", "/api/admin/utilisateurs/(\\d+)/mot-de-passe", "Réinitialisation d'un mot de passe"),
            new Regle("DELETE", "/api/admin/sessions/(\\d+)", "Révocation d'une session"),
            new Regle("PUT", "/api/parametres-cabinet", "Modification des coordonnées du cabinet"));

    private final JournalService journalService;

    @Override
    public void afterCompletion(HttpServletRequest requete, HttpServletResponse reponse, Object handler, Exception exception) {
        String chemin = requete.getRequestURI();
        if (chemin.startsWith("/api/auth/") || reponse.getStatus() >= 400) return;
        boolean modification = !"GET".equals(requete.getMethod()) && !"OPTIONS".equals(requete.getMethod());
        if (!modification && !chemin.endsWith(".pdf")) return;
        try {
            Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
            String email = authentification == null ? null : authentification.getName();
            for (Regle regle : REGLES) {
                Matcher correspondance = regle.motif().matcher(chemin);
                if (regle.methode().equals(requete.getMethod()) && correspondance.matches()) {
                    String description = correspondance.groupCount() > 0 ? "Élément n° " + correspondance.group(1) : null;
                    journalService.enregistrer(email, regle.libelle(), description, requete, reponse.getStatus());
                    return;
                }
            }
            journalService.enregistrer(email, requete.getMethod() + " " + chemin, null, requete, reponse.getStatus());
        } catch (RuntimeException erreur) {
            // Le journal ne doit jamais faire echouer l'action de l'utilisateur.
            log.warn("Journal d'activite non ecrit pour {} : {}", chemin, erreur.getClass().getSimpleName());
        }
    }

    private record Regle(String methode, Pattern motif, String libelle) {
        Regle(String methode, String motif, String libelle) { this(methode, Pattern.compile(motif), libelle); }
    }
}

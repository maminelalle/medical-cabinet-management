package com.cabinetmedical.backend.temps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evenements en temps reel (Server-Sent Events) : chaque onglet ouvert de l'application garde un flux.
 * Un utilisateur est "en ligne" tant qu'il a au moins un flux ouvert ; a l'ouverture du premier flux et a la
 * fermeture du dernier, tous les ecrans sont prevenus (presence des medecins et des employes sans rafraichir).
 */
@Slf4j
@Service
public class TempsReelService {
    /** Sans flux (outil externe, ancien onglet), une activite recente suffit a etre considere en ligne. */
    private static final Duration ACTIVITE_RECENTE = Duration.ofSeconds(90);
    private static final long DUREE_FLUX_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, Abonne> abonnes = new ConcurrentHashMap<>();
    private final Map<Long, Instant> derniereDeconnexion = new ConcurrentHashMap<>();

    /** Ouvre un flux pour l'utilisateur ; previent les autres si c'est son premier onglet ouvert. */
    public SseEmitter abonner(Long utilisateurId, String sessionId, String nomComplet) {
        SseEmitter flux = new SseEmitter(DUREE_FLUX_MS);
        String cle = UUID.randomUUID().toString();
        boolean premier = !estConnecteEnDirect(utilisateurId);
        abonnes.put(cle, new Abonne(flux, utilisateurId, sessionId, nomComplet));
        flux.onCompletion(() -> retirer(cle));
        flux.onTimeout(() -> retirer(cle));
        flux.onError(erreur -> retirer(cle));
        envoyer(cle, new Evenement("BIENVENUE", null, utilisateurId, Instant.now()));
        if (premier) diffuser("EN_LIGNE", nomComplet + " est en ligne", utilisateurId);
        return flux;
    }

    /** Ferme les flux d'une session (deconnexion, revocation) : l'utilisateur passe hors ligne s'il n'en a plus. */
    public void fermerSession(String sessionId) {
        if (sessionId == null) return;
        fermer(abonne -> sessionId.equals(abonne.sessionId()));
    }

    public void fermerUtilisateur(Long utilisateurId) {
        fermer(abonne -> abonne.utilisateurId().equals(utilisateurId));
    }

    /** Termine les flux concernes et les retire aussitot (sans attendre la fin de la requete asynchrone). */
    private void fermer(java.util.function.Predicate<Abonne> critere) {
        abonnes.entrySet().stream().filter(entree -> critere.test(entree.getValue())).map(Map.Entry::getKey).toList()
                .forEach(cle -> {
                    Abonne abonne = abonnes.get(cle);
                    if (abonne == null) return;
                    retirer(cle);
                    abonne.flux().complete();
                });
    }

    /**
     * En ligne : un flux ouvert, ou une activite recente posterieure a sa derniere fermeture de flux
     * (onglet ferme = hors ligne immediatement).
     */
    public boolean estEnLigne(Long utilisateurId, Instant derniereActivite) {
        return estConnecteEnDirect(utilisateurId) || activiteRecente(utilisateurId, derniereActivite);
    }

    /** Activite recente sans flux, et posterieure a la derniere fermeture de flux de l'utilisateur. */
    public boolean activiteRecente(Long utilisateurId, Instant derniereActivite) {
        if (derniereActivite == null || derniereActivite.isBefore(Instant.now().minus(ACTIVITE_RECENTE))) return false;
        Instant deconnexion = derniereDeconnexion.get(utilisateurId);
        return deconnexion == null || derniereActivite.isAfter(deconnexion);
    }

    /** Diffuse apres la validation de la transaction en cours, pour que les ecrans relisent des donnees a jour. */
    public void diffuserApresValidation(String type, String message, Long utilisateurId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { diffuser(type, message, utilisateurId); }
            });
        } else {
            diffuser(type, message, utilisateurId);
        }
    }

    public void diffuser(String type, String message, Long utilisateurId) {
        Evenement evenement = new Evenement(type, message, utilisateurId, Instant.now());
        abonnes.keySet().forEach(cle -> envoyer(cle, evenement));
    }

    /** Toutes les 60 s : les ecrans recalculent les retards ("en retard" depend de l'heure). */
    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void battement() {
        diffuser("BATTEMENT", null, null);
    }

    /**
     * Toutes les 5 s : un commentaire SSE (ignore par le navigateur) detecte les onglets fermes ;
     * l'ecriture echoue sur une connexion coupee et l'utilisateur passe aussitot hors ligne.
     */
    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void verifierConnexions() {
        abonnes.forEach((cle, abonne) -> {
            try {
                abonne.flux().send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException erreur) {
                retirer(cle);
            }
        });
    }

    public int nombreFlux() { return abonnes.size(); }

    /** Une session dont l'application est ouverte en ce moment. */
    public boolean sessionEnDirect(String sessionId) {
        return sessionId != null && abonnes.values().stream().anyMatch(abonne -> sessionId.equals(abonne.sessionId()));
    }

    private boolean estConnecteEnDirect(Long utilisateurId) {
        return abonnes.values().stream().anyMatch(abonne -> abonne.utilisateurId().equals(utilisateurId));
    }

    private void envoyer(String cle, Evenement evenement) {
        Abonne abonne = abonnes.get(cle);
        if (abonne == null) return;
        try {
            abonne.flux().send(SseEmitter.event().name("cabinet").data(evenement));
        } catch (IOException | IllegalStateException erreur) {
            retirer(cle);
        }
    }

    private void retirer(String cle) {
        Abonne abonne = abonnes.remove(cle);
        if (abonne == null) return;
        if (!estConnecteEnDirect(abonne.utilisateurId())) {
            derniereDeconnexion.put(abonne.utilisateurId(), Instant.now());
            diffuser("HORS_LIGNE", abonne.nomComplet() + " n'est plus connecté(e)", abonne.utilisateurId());
        }
    }

    private record Abonne(SseEmitter flux, Long utilisateurId, String sessionId, String nomComplet) {}

    /** Message pousse aux ecrans : type (EN_LIGNE, HORS_LIGNE, CONSULTATION, COMPTES, BATTEMENT...) et texte affiche. */
    public record Evenement(String type, String message, Long utilisateurId, Instant date) {}
}

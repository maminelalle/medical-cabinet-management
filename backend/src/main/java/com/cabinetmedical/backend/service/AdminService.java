package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.admin.AdminDtos.*;
import com.cabinetmedical.backend.dto.admin.UtilisateurRequest;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.repository.JournalActiviteRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.SessionUtilisateurRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import com.cabinetmedical.backend.security.JwtService;
import com.cabinetmedical.backend.security.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/** Administration : comptes, sessions de connexion, journal d'activite et matrice des permissions. */
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UtilisateurRepository utilisateurRepository;
    private final MedecinRepository medecinRepository;
    private final SessionUtilisateurRepository sessionRepository;
    private final JournalActiviteRepository journalRepository;
    private final SessionService sessionService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurConnecte utilisateurConnecte;

    // --- Tableau de bord -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public TableauBordAdminResponse tableauDeBord() {
        Instant debutJour = debutDuJour();
        List<UtilisateurResponse> utilisateurs = utilisateurs();
        List<JournalActivite> journalDuJour = journalRepository.findByDateActionGreaterThanEqualOrderByDateActionDesc(
                debutJour, PageRequest.of(0, 2000));
        return new TableauBordAdminResponse(
                utilisateurs.size(),
                utilisateurs.stream().filter(UtilisateurResponse::actif).count(),
                utilisateurs.stream().filter(UtilisateurResponse::enLigne).count(),
                utilisateurs.stream().filter(UtilisateurResponse::connecteAujourdhui).count(),
                sessionRepository.findByDateFinIsNullOrderByDerniereActiviteDesc().stream().filter(this::nonExpiree).count(),
                journalDuJour.size(),
                journalDuJour.stream().filter(entree -> "Échec de connexion".equals(entree.getAction())).count(),
                utilisateurs,
                journalDuJour.stream().limit(12).map(this::versJournal).toList());
    }

    // --- Comptes ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> utilisateurs() {
        Map<Long, List<SessionUtilisateur>> sessionsOuvertes = sessionRepository.findByDateFinIsNullOrderByDerniereActiviteDesc()
                .stream().filter(this::nonExpiree).collect(Collectors.groupingBy(session -> session.getUtilisateur().getId()));
        Map<Long, Medecin> medecins = medecinRepository.findAll().stream()
                .collect(Collectors.toMap(medecin -> medecin.getUtilisateur().getId(), medecin -> medecin));
        return utilisateurRepository.findAll().stream()
                .sorted(Comparator.comparing((Utilisateur u) -> u.getRole().name()).thenComparing(u -> Objects.toString(u.getNom(), "")))
                .map(utilisateur -> versUtilisateur(utilisateur, sessionsOuvertes.getOrDefault(utilisateur.getId(), List.of()),
                        medecins.get(utilisateur.getId())))
                .toList();
    }

    @Transactional
    public UtilisateurResponse creer(UtilisateurRequest request) {
        if (request.motDePasse() == null || request.motDePasse().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe initial est obligatoire (8 caractères minimum)");
        }
        verifierEmailLibre(request.email(), null);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(request.email().trim().toLowerCase());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setRole(request.role());
        appliquer(utilisateur, request);
        utilisateur = utilisateurRepository.save(utilisateur);
        Medecin medecin = null;
        if (request.role() == Role.MEDECIN) {
            medecin = new Medecin();
            medecin.setUtilisateur(utilisateur);
            appliquer(medecin, request);
            medecin = medecinRepository.save(medecin);
        }
        return versUtilisateur(utilisateur, List.of(), medecin);
    }

    @Transactional
    public UtilisateurResponse modifier(Long id, UtilisateurRequest request, UserDetails connecte) {
        Utilisateur utilisateur = trouver(id);
        if ((utilisateur.getRole() == Role.MEDECIN) != (request.role() == Role.MEDECIN)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le rôle médecin ne peut pas être ajouté ou retiré à un compte existant : créez un nouveau compte");
        }
        if (utilisateur.getId().equals(utilisateurConnecte.utilisateur(connecte).getId()) && request.role() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous ne pouvez pas retirer votre propre rôle administrateur");
        }
        verifierEmailLibre(request.email(), id);
        // Un changement de role ou d'email deconnecte l'utilisateur : son jeton porte l'ancien role.
        boolean identiteModifiee = utilisateur.getRole() != request.role() || !utilisateur.getEmail().equalsIgnoreCase(request.email().trim());
        utilisateur.setEmail(request.email().trim().toLowerCase());
        utilisateur.setRole(request.role());
        appliquer(utilisateur, request);
        Medecin medecin = medecinRepository.findByUtilisateurId(id).orElse(null);
        if (medecin != null) appliquer(medecin, request);
        if (identiteModifiee) sessionService.fermerToutes(id, SessionUtilisateur.FIN_REVOQUEE);
        return versUtilisateur(utilisateurRepository.save(utilisateur), sessionsOuvertes(id), medecin);
    }

    /** Desactiver un compte ferme toutes ses sessions : l'utilisateur est deconnecte immediatement. */
    @Transactional
    public UtilisateurResponse changerStatut(Long id, boolean actif, UserDetails connecte) {
        Utilisateur utilisateur = trouver(id);
        if (!actif && utilisateur.getId().equals(utilisateurConnecte.utilisateur(connecte).getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous ne pouvez pas désactiver votre propre compte");
        }
        utilisateur.setActif(actif);
        if (!actif) sessionService.fermerToutes(id, SessionUtilisateur.FIN_REVOQUEE);
        return versUtilisateur(utilisateurRepository.save(utilisateur), sessionsOuvertes(id),
                medecinRepository.findByUtilisateurId(id).orElse(null));
    }

    @Transactional
    public void reinitialiserMotDePasse(Long id, String motDePasse) {
        Utilisateur utilisateur = trouver(id);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateurRepository.save(utilisateur);
        sessionService.fermerToutes(id, SessionUtilisateur.FIN_REVOQUEE);
    }

    // --- Sessions et journal ---------------------------------------------------------------------

    /** {@code periode} : "actives" (sessions ouvertes), "jour" ou "semaine" (connexions de la periode). */
    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(String periode) {
        List<SessionUtilisateur> sessions = switch (periode == null ? "jour" : periode) {
            case "actives" -> sessionRepository.findByDateFinIsNullOrderByDerniereActiviteDesc().stream().filter(this::nonExpiree).toList();
            case "semaine" -> sessionRepository.findByDateConnexionGreaterThanEqualOrderByDateConnexionDesc(debutDuJour().minus(Duration.ofDays(6)));
            default -> sessionRepository.findByDateConnexionGreaterThanEqualOrderByDateConnexionDesc(debutDuJour());
        };
        return sessions.stream().map(this::versSession).toList();
    }

    @Transactional
    public void revoquerSession(Long id) {
        SessionUtilisateur session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session introuvable"));
        sessionService.fermer(session.getJetonId(), SessionUtilisateur.FIN_REVOQUEE);
    }

    @Transactional(readOnly = true)
    public List<JournalResponse> journal(Long utilisateurId, Integer jours, Integer limite) {
        Instant depuis = debutDuJour().minus(Duration.ofDays(Math.max(0, (jours == null ? 7 : jours) - 1)));
        PageRequest page = PageRequest.of(0, Math.min(limite == null ? 300 : limite, 2000));
        List<JournalActivite> entrees = utilisateurId == null
                ? journalRepository.findByDateActionGreaterThanEqualOrderByDateActionDesc(depuis, page)
                : journalRepository.findByUtilisateurIdAndDateActionGreaterThanEqualOrderByDateActionDesc(utilisateurId, depuis, page);
        return entrees.stream().map(this::versJournal).toList();
    }

    /** Matrice des droits telle qu'elle est appliquee par le serveur (@PreAuthorize). */
    public List<PermissionResponse> permissions() {
        return List.of(
                new PermissionResponse(Role.ACCUEIL, "Accueil / Caisse", List.of(
                        "Patients : création, modification, import et export de dossier",
                        "Rendez-vous : prise (avec création rapide du patient), modification, annulation, suppression",
                        "Factures : création liée au rendez-vous ou à l'acte, encaissement avec référence, annulation",
                        "Actes programmés : consultation et annulation, facturation",
                        "Dossier patient sans compte-rendu médical, présence des médecins")),
                new PermissionResponse(Role.MEDECIN, "Médecin", List.of(
                        "Son planning : démarrer et terminer une consultation",
                        "Compte-rendu, ordonnance (recherche du stock par nom ou famille)",
                        "Programmation d'actes (chirurgie, traitement...) et compte-rendu de réalisation",
                        "Dossiers de ses patients uniquement")),
                new PermissionResponse(Role.PHARMACIEN, "Pharmacien", List.of(
                        "Stock : ajout, import / export, approvisionnement, correction d'inventaire",
                        "Fiche produit : ventes, achats et mouvements",
                        "Vente des ordonnances avec paiement obligatoire et référence",
                        "Finances de la pharmacie")),
                new PermissionResponse(Role.DIRECTION, "Direction", List.of(
                        "Tableau de bord : activité, chiffre d'affaires, impayés",
                        "Lecture : patients, dossiers, rendez-vous, actes, factures, pharmacie",
                        "Catalogue des actes, présence des médecins")),
                new PermissionResponse(Role.ADMIN, "Administrateur", List.of(
                        "Comptes : création, modification, activation, mot de passe",
                        "Sessions et appareils connectés, révocation",
                        "Journal d'activité de tous les utilisateurs",
                        "Coordonnées du cabinet, lecture de toutes les données (sans comptes-rendus médicaux)")));
    }

    // --- Outils ----------------------------------------------------------------------------------

    private UtilisateurResponse versUtilisateur(Utilisateur utilisateur, List<SessionUtilisateur> ouvertes, Medecin medecin) {
        Instant derniereActivite = ouvertes.stream().map(SessionUtilisateur::getDerniereActivite).max(Comparator.naturalOrder()).orElse(null);
        boolean enLigne = derniereActivite != null && derniereActivite.isAfter(Instant.now().minus(SessionService.EN_LIGNE));
        boolean aujourdhui = utilisateur.getDerniereConnexion() != null && !utilisateur.getDerniereConnexion().isBefore(debutDuJour());
        return new UtilisateurResponse(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole(), utilisateur.getNom(),
                utilisateur.getPrenom(), utilisateur.getTelephone(), utilisateur.isActif(), utilisateur.getDerniereConnexion(),
                derniereActivite, enLigne, aujourdhui, ouvertes.size(), medecin == null ? null : medecin.getId(),
                medecin == null ? null : medecin.getSpecialite(), medecin == null ? null : medecin.getNumeroOrdre(),
                utilisateur.getCreatedAt());
    }

    private SessionResponse versSession(SessionUtilisateur session) {
        Utilisateur utilisateur = session.getUtilisateur();
        String statut = session.getDateFin() != null ? session.getMotifFin()
                : !nonExpiree(session) ? SessionUtilisateur.FIN_EXPIREE
                : session.getDerniereActivite().isAfter(Instant.now().minus(SessionService.EN_LIGNE)) ? "EN_LIGNE" : "INACTIVE";
        return new SessionResponse(session.getId(), utilisateur.getId(), utilisateur.getEmail(), nomComplet(utilisateur),
                utilisateur.getRole(), session.getAdresseIp(), session.getAppareil(), session.getNavigateur(), session.getSysteme(),
                session.getDateConnexion(), session.getDerniereActivite(), session.getDateFin(), statut);
    }

    private JournalResponse versJournal(JournalActivite entree) {
        return new JournalResponse(entree.getId(), entree.getEmail(),
                entree.getUtilisateur() == null ? null : nomComplet(entree.getUtilisateur()), entree.getRole(), entree.getAction(),
                entree.getDescription(), entree.getMethode(), entree.getChemin(), entree.getStatutHttp(), entree.getAdresseIp(),
                entree.getDateAction());
    }

    /** Une session sans deconnexion expire avec son jeton. */
    private boolean nonExpiree(SessionUtilisateur session) {
        return session.getDateConnexion().plusMillis(jwtService.getExpirationMs()).isAfter(Instant.now());
    }

    private List<SessionUtilisateur> sessionsOuvertes(Long utilisateurId) {
        return sessionRepository.findByUtilisateurIdAndDateFinIsNull(utilisateurId).stream().filter(this::nonExpiree).toList();
    }

    private void verifierEmailLibre(String email, Long idCourant) {
        utilisateurRepository.findByEmailIgnoreCase(email.trim()).filter(existant -> !existant.getId().equals(idCourant))
                .ifPresent(existant -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte utilise déjà cet email");
                });
    }

    private static void appliquer(Utilisateur utilisateur, UtilisateurRequest request) {
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setTelephone(request.telephone() == null || request.telephone().isBlank() ? null : request.telephone().trim());
    }

    private static void appliquer(Medecin medecin, UtilisateurRequest request) {
        medecin.setNom(request.nom().trim());
        medecin.setPrenom(request.prenom().trim());
        medecin.setSpecialite(request.specialite() == null || request.specialite().isBlank() ? null : request.specialite().trim());
        medecin.setNumeroOrdre(request.numeroOrdre() == null || request.numeroOrdre().isBlank() ? null : request.numeroOrdre().trim());
    }

    static String nomComplet(Utilisateur utilisateur) {
        String nom = (Objects.toString(utilisateur.getPrenom(), "") + " " + Objects.toString(utilisateur.getNom(), "")).trim();
        return nom.isEmpty() ? utilisateur.getEmail() : nom;
    }

    private static Instant debutDuJour() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Utilisateur trouver(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}

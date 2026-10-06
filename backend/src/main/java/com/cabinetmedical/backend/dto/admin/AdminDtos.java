package com.cabinetmedical.backend.dto.admin;

import com.cabinetmedical.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/** Objets echanges par l'espace d'administration. */
public final class AdminDtos {
    private AdminDtos() {}

    public record UtilisateurResponse(Long id, String email, Role role, String nom, String prenom, String telephone,
                                      boolean actif, Instant derniereConnexion, Instant derniereActivite, boolean enLigne,
                                      boolean connecteAujourdhui, int sessionsOuvertes, Long medecinId, String specialite,
                                      String numeroOrdre, Instant createdAt) {}

    public record StatutCompteRequest(@NotNull Boolean actif) {}

    public record MotDePasseRequest(@NotBlank @Size(min = 8, max = 100) String motDePasse) {}

    /** statut : EN_LIGNE, INACTIVE, EXPIREE, DECONNEXION, REVOQUEE. */
    public record SessionResponse(Long id, Long utilisateurId, String email, String nomComplet, Role role, String adresseIp,
                                  String appareil, String navigateur, String systeme, Instant dateConnexion,
                                  Instant derniereActivite, Instant dateFin, String statut) {}

    public record JournalResponse(Long id, String email, String nomComplet, String role, String action, String description,
                                  String methode, String chemin, Integer statutHttp, String adresseIp, Instant dateAction) {}

    public record TableauBordAdminResponse(long utilisateurs, long utilisateursActifs, long enLigne, long connectesAujourdhui,
                                           long sessionsOuvertes, long actionsAujourdhui, long echecsConnexionAujourdhui,
                                           List<UtilisateurResponse> presence, List<JournalResponse> dernieresActions) {}

    public record PermissionResponse(Role role, String libelle, List<String> acces) {}

    /** statut : ABSENT, DISPONIBLE, EN_CONSULTATION, EN_RETARD. */
    public record PresenceMedecinResponse(Long medecinId, String nom, String prenom, String specialite, boolean connecte,
                                          Instant derniereActivite, String statut, String patientEnCours,
                                          Instant debutConsultation, LocalDateTime prochainRendezVous, String prochainPatient,
                                          long retardMinutes, long rendezVousRestants, long rendezVousTermines) {}
}

package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.admin.AdminDtos.*;
import com.cabinetmedical.backend.dto.admin.UtilisateurRequest;
import com.cabinetmedical.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administration. Les comptes des employes et leurs sessions sont geres par l'administrateur et par la direction
 * (sans acces aux comptes administrateur) ; le journal, le tableau de bord et les permissions restent a l'administrateur.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private static final String GESTION_EMPLOYES = "hasAnyRole('ADMIN', 'DIRECTION')";

    private final AdminService service;

    @GetMapping("/tableau-de-bord")
    @PreAuthorize("hasRole('ADMIN')")
    public TableauBordAdminResponse tableauDeBord() { return service.tableauDeBord(); }

    @GetMapping("/utilisateurs")
    @PreAuthorize(GESTION_EMPLOYES)
    public List<UtilisateurResponse> utilisateurs(@AuthenticationPrincipal UserDetails connecte) { return service.utilisateurs(connecte); }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(GESTION_EMPLOYES)
    public UtilisateurResponse creer(@Valid @RequestBody UtilisateurRequest request, @AuthenticationPrincipal UserDetails connecte) {
        return service.creer(request, connecte);
    }

    @PutMapping("/utilisateurs/{id}")
    @PreAuthorize(GESTION_EMPLOYES)
    public UtilisateurResponse modifier(@PathVariable Long id, @Valid @RequestBody UtilisateurRequest request,
                                        @AuthenticationPrincipal UserDetails connecte) {
        return service.modifier(id, request, connecte);
    }

    @DeleteMapping("/utilisateurs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTION_EMPLOYES)
    public void supprimer(@PathVariable Long id, @AuthenticationPrincipal UserDetails connecte) { service.supprimer(id, connecte); }

    @PatchMapping("/utilisateurs/{id}/statut")
    @PreAuthorize(GESTION_EMPLOYES)
    public UtilisateurResponse statut(@PathVariable Long id, @Valid @RequestBody StatutCompteRequest request,
                                      @AuthenticationPrincipal UserDetails connecte) {
        return service.changerStatut(id, request.actif(), connecte);
    }

    @PostMapping("/utilisateurs/{id}/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTION_EMPLOYES)
    public void motDePasse(@PathVariable Long id, @Valid @RequestBody MotDePasseRequest request,
                           @AuthenticationPrincipal UserDetails connecte) {
        service.reinitialiserMotDePasse(id, request.motDePasse(), connecte);
    }

    @GetMapping("/sessions")
    @PreAuthorize(GESTION_EMPLOYES)
    public List<SessionResponse> sessions(@RequestParam(required = false) String periode, @AuthenticationPrincipal UserDetails connecte) {
        return service.sessions(periode, connecte);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(GESTION_EMPLOYES)
    public void revoquer(@PathVariable Long id, @AuthenticationPrincipal UserDetails connecte) { service.revoquerSession(id, connecte); }

    @GetMapping("/journal")
    @PreAuthorize("hasRole('ADMIN')")
    public List<JournalResponse> journal(@RequestParam(required = false) Long utilisateurId,
                                         @RequestParam(required = false) Integer jours,
                                         @RequestParam(required = false) Integer limite) {
        return service.journal(utilisateurId, jours, limite);
    }

    @GetMapping("/permissions")
    @PreAuthorize(GESTION_EMPLOYES)
    public List<PermissionResponse> permissions() { return service.permissions(); }
}

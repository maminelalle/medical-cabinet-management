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

/** Espace d'administration : reserve au role ADMIN. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService service;

    @GetMapping("/tableau-de-bord")
    public TableauBordAdminResponse tableauDeBord() { return service.tableauDeBord(); }

    @GetMapping("/utilisateurs")
    public List<UtilisateurResponse> utilisateurs() { return service.utilisateurs(); }

    @PostMapping("/utilisateurs")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse creer(@Valid @RequestBody UtilisateurRequest request) { return service.creer(request); }

    @PutMapping("/utilisateurs/{id}")
    public UtilisateurResponse modifier(@PathVariable Long id, @Valid @RequestBody UtilisateurRequest request,
                                        @AuthenticationPrincipal UserDetails connecte) {
        return service.modifier(id, request, connecte);
    }

    @PatchMapping("/utilisateurs/{id}/statut")
    public UtilisateurResponse statut(@PathVariable Long id, @Valid @RequestBody StatutCompteRequest request,
                                      @AuthenticationPrincipal UserDetails connecte) {
        return service.changerStatut(id, request.actif(), connecte);
    }

    @PostMapping("/utilisateurs/{id}/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void motDePasse(@PathVariable Long id, @Valid @RequestBody MotDePasseRequest request) {
        service.reinitialiserMotDePasse(id, request.motDePasse());
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@RequestParam(required = false) String periode) { return service.sessions(periode); }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoquer(@PathVariable Long id) { service.revoquerSession(id); }

    @GetMapping("/journal")
    public List<JournalResponse> journal(@RequestParam(required = false) Long utilisateurId,
                                         @RequestParam(required = false) Integer jours,
                                         @RequestParam(required = false) Integer limite) {
        return service.journal(utilisateurId, jours, limite);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() { return service.permissions(); }
}

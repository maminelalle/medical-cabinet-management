package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.RendezVousRequest;
import com.cabinetmedical.backend.dto.RendezVousResponse;
import com.cabinetmedical.backend.dto.StatutRendezVousRequest;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.service.RendezVousService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rendezvous")
@RequiredArgsConstructor
public class RendezVousController {
    private final RendezVousService rendezVousService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION')")
    public List<RendezVousResponse> rechercher(
            @RequestParam(required = false) Long medecinId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutRendezVous statut,
            @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.rechercher(medecinId, date, dateDebut, dateFin, statut, utilisateurConnecte);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION')")
    public RendezVousResponse trouver(@PathVariable Long id) {
        return rendezVousService.trouverRendezVous(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ACCUEIL')")
    public RendezVousResponse creer(@Valid @RequestBody RendezVousRequest request,
                                    @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.creer(request, utilisateurConnecte);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACCUEIL')")
    public RendezVousResponse modifier(@PathVariable Long id, @Valid @RequestBody RendezVousRequest request) {
        return rendezVousService.modifier(id, request);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN')")
    public RendezVousResponse changerStatut(@PathVariable Long id,
                                            @Valid @RequestBody StatutRendezVousRequest request,
                                            @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.changerStatut(id, request.statut(), utilisateurConnecte);
    }
}
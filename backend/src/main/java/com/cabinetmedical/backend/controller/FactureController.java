package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.FactureRequest;
import com.cabinetmedical.backend.dto.FactureResponse;
import com.cabinetmedical.backend.dto.PaiementRequest;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.service.FactureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureController {
    private final FactureService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'DIRECTION')")
    public List<FactureResponse> lister(@RequestParam(required = false) StatutFacture statut) { return service.lister(statut); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'DIRECTION')")
    public FactureResponse trouver(@PathVariable Long id) { return service.trouver(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ACCUEIL')")
    public FactureResponse creer(@Valid @RequestBody FactureRequest request,
                                 @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return service.creer(request, utilisateurConnecte);
    }

    @PostMapping("/{id}/paiements")
    @PreAuthorize("hasRole('ACCUEIL')")
    public FactureResponse ajouterPaiement(@PathVariable Long id, @Valid @RequestBody PaiementRequest request,
                                           @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return service.ajouterPaiement(id, request, utilisateurConnecte);
    }
}
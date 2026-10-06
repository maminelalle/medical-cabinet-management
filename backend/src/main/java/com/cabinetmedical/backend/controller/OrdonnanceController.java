package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.OrdonnanceResponse;
import com.cabinetmedical.backend.service.OrdonnanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordonnances")
@RequiredArgsConstructor
public class OrdonnanceController {
    private final OrdonnanceService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public List<OrdonnanceResponse> lister(@RequestParam(required = false) Long patientId,
                                           @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return service.lister(patientId, utilisateurConnecte);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public OrdonnanceResponse trouver(@PathVariable Long id, @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return service.trouver(id, utilisateurConnecte);
    }
}

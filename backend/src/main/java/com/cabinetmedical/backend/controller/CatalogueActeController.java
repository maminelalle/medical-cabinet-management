package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.CatalogueActeRequest;
import com.cabinetmedical.backend.dto.CatalogueActeResponse;
import com.cabinetmedical.backend.service.CatalogueActeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Grille tarifaire : lecture pour la facturation, gestion complete par la direction. */
@RestController
@RequestMapping("/api/actes-catalogue")
@RequiredArgsConstructor
public class CatalogueActeController {
    private final CatalogueActeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<CatalogueActeResponse> lister(@RequestParam(defaultValue = "false") boolean tous) { return service.lister(tous); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DIRECTION')")
    public CatalogueActeResponse creer(@Valid @RequestBody CatalogueActeRequest request) { return service.creer(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTION')")
    public CatalogueActeResponse modifier(@PathVariable Long id, @Valid @RequestBody CatalogueActeRequest request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('DIRECTION')")
    public void supprimer(@PathVariable Long id) { service.supprimer(id); }
}

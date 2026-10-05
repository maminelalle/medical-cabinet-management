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

@RestController
@RequestMapping("/api/actes-catalogue")
@RequiredArgsConstructor
public class CatalogueActeController {
    private final CatalogueActeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION')")
    public List<CatalogueActeResponse> lister() { return service.lister(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DIRECTION')")
    public CatalogueActeResponse creer(@Valid @RequestBody CatalogueActeRequest request) { return service.creer(request); }
}
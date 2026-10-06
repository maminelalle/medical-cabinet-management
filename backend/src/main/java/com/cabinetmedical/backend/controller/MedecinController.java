package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.MedecinResponse;
import com.cabinetmedical.backend.dto.admin.AdminDtos.PresenceMedecinResponse;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medecins")
@RequiredArgsConstructor
public class MedecinController {
    private final MedecinRepository repository;
    private final PresenceService presenceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<MedecinResponse> lister() { return repository.findAll().stream().map(MedecinResponse::from).toList(); }

    /** Medecins presents au cabinet : connectes, en consultation, disponibles ou en retard. */
    @GetMapping("/presence")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'DIRECTION', 'ADMIN')")
    public List<PresenceMedecinResponse> presence() { return presenceService.presenceMedecins(); }
}

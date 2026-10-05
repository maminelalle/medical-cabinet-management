package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.MedecinResponse;
import com.cabinetmedical.backend.repository.MedecinRepository;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION')")
    public List<MedecinResponse> lister() { return repository.findAll().stream().map(MedecinResponse::from).toList(); }
}
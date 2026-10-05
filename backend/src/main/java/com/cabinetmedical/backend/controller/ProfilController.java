package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ProfilResponse;
import com.cabinetmedical.backend.service.ProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilController {
    private final ProfilService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ProfilResponse profil(@AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return service.profil(utilisateurConnecte);
    }
}
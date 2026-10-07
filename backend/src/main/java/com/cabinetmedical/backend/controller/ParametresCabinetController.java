package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ParametresCabinetDto;
import com.cabinetmedical.backend.dto.RegleControleGratuitDto;
import com.cabinetmedical.backend.service.ControleGratuitService;
import com.cabinetmedical.backend.service.ParametresCabinetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres-cabinet")
@RequiredArgsConstructor
public class ParametresCabinetController {
    private final ParametresCabinetService service;
    private final ControleGratuitService controleGratuitService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ParametresCabinetDto lire() { return service.lire(); }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ParametresCabinetDto modifier(@Valid @RequestBody ParametresCabinetDto request) { return service.modifier(request); }

    /** Regle du controle gratuit (delai et nombre), affichee a l'accueil et reglee par la direction. */
    @GetMapping("/controle-gratuit")
    @PreAuthorize("isAuthenticated()")
    public RegleControleGratuitDto regleControle() { return controleGratuitService.regle(); }

    @PutMapping("/controle-gratuit")
    @PreAuthorize("hasAnyRole('DIRECTION', 'ADMIN')")
    public RegleControleGratuitDto modifierRegleControle(@Valid @RequestBody RegleControleGratuitDto request) {
        return controleGratuitService.modifierRegle(request);
    }
}

package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ParametresCabinetDto;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ParametresCabinetDto lire() { return service.lire(); }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ParametresCabinetDto modifier(@Valid @RequestBody ParametresCabinetDto request) { return service.modifier(request); }
}

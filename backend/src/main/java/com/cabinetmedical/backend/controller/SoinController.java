package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.FinSoinRequest;
import com.cabinetmedical.backend.dto.MotifRequest;
import com.cabinetmedical.backend.dto.SoinRequest;
import com.cabinetmedical.backend.dto.SoinResponse;
import com.cabinetmedical.backend.entity.StatutSoin;
import com.cabinetmedical.backend.service.SoinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Soins (injection, perfusion...) : l'accueil et les medecins les gerent, la direction les consulte. */
@RestController
@RequestMapping("/api/soins")
@RequiredArgsConstructor
public class SoinController {
    private static final String EQUIPE_SOINS = "hasAnyRole('ACCUEIL', 'MEDECIN')";

    private final SoinService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<SoinResponse> lister(@RequestParam(required = false) LocalDate date,
                                     @RequestParam(required = false) Long patientId,
                                     @RequestParam(required = false) StatutSoin statut) {
        return service.lister(date, patientId, statut);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public SoinResponse trouver(@PathVariable Long id) { return service.trouver(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(EQUIPE_SOINS)
    public SoinResponse creer(@Valid @RequestBody SoinRequest request, @AuthenticationPrincipal UserDetails connecte) {
        return service.creer(request, connecte);
    }

    @PutMapping("/{id}")
    @PreAuthorize(EQUIPE_SOINS)
    public SoinResponse modifier(@PathVariable Long id, @Valid @RequestBody SoinRequest request) {
        return service.modifier(id, request);
    }

    @PostMapping("/{id}/demarrer")
    @PreAuthorize(EQUIPE_SOINS)
    public SoinResponse demarrer(@PathVariable Long id, @AuthenticationPrincipal UserDetails connecte) {
        return service.demarrer(id, connecte);
    }

    @PostMapping("/{id}/terminer")
    @PreAuthorize(EQUIPE_SOINS)
    public SoinResponse terminer(@PathVariable Long id, @RequestBody(required = false) FinSoinRequest request,
                                 @AuthenticationPrincipal UserDetails connecte) {
        return service.terminer(id, request, connecte);
    }

    @PostMapping("/{id}/annulation")
    @PreAuthorize(EQUIPE_SOINS)
    public SoinResponse annuler(@PathVariable Long id, @RequestBody(required = false) MotifRequest request) {
        return service.annuler(id, request);
    }
}

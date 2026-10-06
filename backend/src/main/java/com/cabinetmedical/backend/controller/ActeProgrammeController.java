package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ActeProgrammeRequest;
import com.cabinetmedical.backend.dto.ActeProgrammeResponse;
import com.cabinetmedical.backend.dto.MotifRequest;
import com.cabinetmedical.backend.dto.RealisationActeRequest;
import com.cabinetmedical.backend.entity.StatutActe;
import com.cabinetmedical.backend.service.ActeProgrammeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actes")
@RequiredArgsConstructor
public class ActeProgrammeController {
    private final ActeProgrammeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<ActeProgrammeResponse> lister(@RequestParam(required = false) Long patientId,
                                              @RequestParam(required = false) StatutActe statut,
                                              @AuthenticationPrincipal UserDetails connecte) {
        return service.lister(patientId, statut, connecte);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public ActeProgrammeResponse trouver(@PathVariable Long id, @AuthenticationPrincipal UserDetails connecte) {
        return service.trouver(id, connecte);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDECIN')")
    public ActeProgrammeResponse programmer(@Valid @RequestBody ActeProgrammeRequest request,
                                            @AuthenticationPrincipal UserDetails connecte) {
        return service.programmer(request, connecte);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MEDECIN')")
    public ActeProgrammeResponse modifier(@PathVariable Long id, @Valid @RequestBody ActeProgrammeRequest request,
                                          @AuthenticationPrincipal UserDetails connecte) {
        return service.modifier(id, request, connecte);
    }

    @PostMapping("/{id}/realisation")
    @PreAuthorize("hasRole('MEDECIN')")
    public ActeProgrammeResponse realiser(@PathVariable Long id, @Valid @RequestBody RealisationActeRequest request,
                                          @AuthenticationPrincipal UserDetails connecte) {
        return service.realiser(id, request, connecte);
    }

    @PostMapping("/{id}/annulation")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ACCUEIL')")
    public ActeProgrammeResponse annuler(@PathVariable Long id, @Valid @RequestBody MotifRequest request,
                                         @AuthenticationPrincipal UserDetails connecte) {
        return service.annuler(id, request, connecte);
    }
}

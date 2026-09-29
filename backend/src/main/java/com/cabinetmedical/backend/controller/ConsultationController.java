package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ConsultationRequest;
import com.cabinetmedical.backend.dto.ConsultationResponse;
import com.cabinetmedical.backend.dto.PrescriptionRequest;
import com.cabinetmedical.backend.dto.PrescriptionResponse;
import com.cabinetmedical.backend.service.ConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsultationController {
    private final ConsultationService consultationService;

    @PostMapping("/rendezvous/{rendezVousId}/consultation")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDECIN')")
    public ConsultationResponse creer(@PathVariable Long rendezVousId, @Valid @RequestBody ConsultationRequest request,
                                      @AuthenticationPrincipal UserDetails medecinConnecte) {
        return consultationService.creer(rendezVousId, request, medecinConnecte);
    }

    @GetMapping("/consultations/{id}")
    @PreAuthorize("hasRole('MEDECIN')")
    public ConsultationResponse trouver(@PathVariable Long id, @AuthenticationPrincipal UserDetails medecinConnecte) {
        return consultationService.trouver(id, medecinConnecte);
    }

    @PostMapping("/consultations/{id}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDECIN')")
    public PrescriptionResponse ajouterPrescription(@PathVariable Long id, @Valid @RequestBody PrescriptionRequest request,
                                                     @AuthenticationPrincipal UserDetails medecinConnecte) {
        return consultationService.ajouterPrescription(id, request, medecinConnecte);
    }
}
package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.DossierPatientResponse;
import com.cabinetmedical.backend.dto.PatientRequest;
import com.cabinetmedical.backend.dto.PatientResponse;
import com.cabinetmedical.backend.service.DossierService;
import com.cabinetmedical.backend.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;
    private final DossierService dossierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN')")
    public List<PatientResponse> rechercher(@RequestParam(required = false) String q) {
        return patientService.rechercher(q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN')")
    public PatientResponse trouver(@PathVariable Long id) {
        return patientService.trouver(id);
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasRole('MEDECIN')")
    public DossierPatientResponse historique(@PathVariable Long id,
                                             @AuthenticationPrincipal UserDetails medecinConnecte) {
        return dossierService.dossier(id, medecinConnecte);
    }

    @PostMapping
    @PreAuthorize("hasRole('ACCUEIL')")
    public PatientResponse creer(@Valid @RequestBody PatientRequest request) {
        return patientService.creer(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACCUEIL')")
    public PatientResponse modifier(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACCUEIL')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        patientService.supprimer(id);
    }
}

package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.service.PharmacieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacie")
@RequiredArgsConstructor
public class PharmacieController {
    private final PharmacieService service;

    @GetMapping("/medicaments")
    @PreAuthorize("hasAnyRole('MEDECIN', 'PHARMACIEN', 'DIRECTION')")
    public List<MedicamentResponse> medicaments() { return service.medicaments(); }

    @PostMapping("/medicaments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse creer(@Valid @RequestBody MedicamentRequest request) { return service.creerMedicament(request); }

    @PostMapping("/medicaments/import")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public List<MedicamentResponse> importer(@Valid @RequestBody List<@Valid MedicamentRequest> requests) {
        return service.importerMedicaments(requests);
    }

    @PatchMapping("/medicaments/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse stock(@PathVariable Long id, @Valid @RequestBody StockRequest request) { return service.modifierStock(id, request); }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public List<PrescriptionPharmacieResponse> prescriptions() { return service.prescriptionsDisponibles(); }

    @PostMapping("/dispensations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PHARMACIEN')")
    public DispensationResponse dispenser(@Valid @RequestBody DispensationRequest request,
                                          @AuthenticationPrincipal UserDetails utilisateur) {
        return service.dispenser(request, utilisateur);
    }
}
package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.service.PharmacieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pharmacie")
@RequiredArgsConstructor
public class PharmacieController {
    private final PharmacieService service;

    @GetMapping("/medicaments")
    @PreAuthorize("hasAnyRole('MEDECIN', 'PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public List<MedicamentResponse> medicaments() { return service.medicaments(); }

    @GetMapping("/medicaments/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public MedicamentDetailResponse details(@PathVariable Long id) { return service.details(id); }

    @GetMapping("/medicaments/inventaire.pdf")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public ResponseEntity<byte[]> inventairePdf() {
        return Pdf.reponse(service.inventairePdf(), "inventaire-pharmacie-" + LocalDate.now() + ".pdf");
    }

    @PostMapping("/medicaments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse creer(@Valid @RequestBody MedicamentRequest request, @AuthenticationPrincipal UserDetails connecte) {
        return service.creerMedicament(request, connecte);
    }

    @PutMapping("/medicaments/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse modifier(@PathVariable Long id, @Valid @RequestBody MedicamentRequest request) {
        return service.modifierMedicament(id, request);
    }

    @PostMapping("/medicaments/import")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public List<MedicamentResponse> importer(@Valid @RequestBody List<@Valid MedicamentRequest> requests,
                                             @AuthenticationPrincipal UserDetails connecte) {
        return service.importerMedicaments(requests, connecte);
    }

    @PatchMapping("/medicaments/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse stock(@PathVariable Long id, @Valid @RequestBody StockRequest request,
                                    @AuthenticationPrincipal UserDetails connecte) {
        return service.modifierStock(id, request, connecte);
    }

    @PostMapping("/medicaments/{id}/approvisionnements")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION')")
    public MedicamentResponse approvisionner(@PathVariable Long id, @Valid @RequestBody ApprovisionnementRequest request,
                                             @AuthenticationPrincipal UserDetails connecte) {
        return service.approvisionner(id, request, connecte);
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public List<PrescriptionPharmacieResponse> prescriptions() { return service.prescriptionsDisponibles(); }

    @PostMapping("/dispensations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PHARMACIEN')")
    public DispensationResponse dispenser(@Valid @RequestBody DispensationRequest request,
                                          @AuthenticationPrincipal UserDetails connecte) {
        return service.dispenser(request, connecte);
    }

    @GetMapping("/finances")
    @PreAuthorize("hasAnyRole('PHARMACIEN', 'DIRECTION', 'ADMIN')")
    public PharmacieFinancesResponse finances(@RequestParam(required = false) LocalDate dateDebut,
                                              @RequestParam(required = false) LocalDate dateFin) {
        return service.finances(dateDebut, dateFin);
    }
}

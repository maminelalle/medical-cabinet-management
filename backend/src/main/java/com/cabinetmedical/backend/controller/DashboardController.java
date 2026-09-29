package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ChiffreAffairesResponse;
import com.cabinetmedical.backend.dto.DashboardConsultationsResponse;
import com.cabinetmedical.backend.dto.ImpayesResponse;
import com.cabinetmedical.backend.dto.MedecinActiviteResponse;
import com.cabinetmedical.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/consultations")
    @PreAuthorize("hasRole('DIRECTION')")
    public DashboardConsultationsResponse consultations(@RequestParam(required = false) LocalDate dateDebut,
                                                        @RequestParam(required = false) LocalDate dateFin) {
        return service.consultations(dateDebut, dateFin);
    }

    @GetMapping("/chiffre-affaires")
    @PreAuthorize("hasRole('DIRECTION')")
    public ChiffreAffairesResponse chiffreAffaires(@RequestParam(required = false) LocalDate dateDebut,
                                                   @RequestParam(required = false) LocalDate dateFin) {
        return service.chiffreAffaires(dateDebut, dateFin);
    }

    @GetMapping("/impayes")
    @PreAuthorize("hasRole('DIRECTION')")
    public ImpayesResponse impayes() {
        return service.impayes();
    }

    @GetMapping("/activite-medecins")
    @PreAuthorize("hasRole('DIRECTION')")
    public List<MedecinActiviteResponse> activiteMedecins(@RequestParam(required = false) LocalDate dateDebut,
                                                          @RequestParam(required = false) LocalDate dateFin) {
        return service.activiteMedecins(dateDebut, dateFin);
    }
}
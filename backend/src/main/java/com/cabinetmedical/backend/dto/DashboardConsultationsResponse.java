package com.cabinetmedical.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardConsultationsResponse(
        LocalDate dateDebut,
        LocalDate dateFin,
        long consultations,
        long rendezVous,
        long rendezVousTermines,
        List<MedecinActiviteResponse> parMedecin
) {}
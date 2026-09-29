package com.cabinetmedical.backend.dto;

import java.util.List;

public record DossierPatientResponse(
        PatientResponse patient,
        List<ConsultationDossierResponse> consultations,
        List<RendezVousResponse> prochainsRendezVous
) {}
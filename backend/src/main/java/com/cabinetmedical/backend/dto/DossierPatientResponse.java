package com.cabinetmedical.backend.dto;

import java.util.List;

/**
 * Dossier patient complet : identite, consultations avec ordonnances, historique complet des rendez-vous,
 * prochains rendez-vous et factures avec leurs paiements. Sert aussi de format d'export.
 */
public record DossierPatientResponse(
        PatientResponse patient,
        List<ConsultationDossierResponse> consultations,
        List<RendezVousResponse> prochainsRendezVous,
        List<RendezVousResponse> rendezVous,
        List<FactureResponse> factures,
        List<ActeProgrammeResponse> actes,
        boolean compteRenduMasque
) {}

package com.cabinetmedical.backend.dto;

/** Bilan d'un import de dossier patient. */
public record DossierImportResponse(
        Long patientId,
        boolean patientCree,
        int consultationsImportees,
        int ordonnancesImportees,
        int rendezVousImportes,
        int facturesImportees,
        int elementsIgnores
) {}

package com.cabinetmedical.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImpayesResponse(
        long nombreFactures,
        long nombreImpayees,
        BigDecimal montantFacture,
        BigDecimal montantRestant,
        int tauxImpayees,
        List<FactureResponse> factures
) {}
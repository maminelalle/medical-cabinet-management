package com.cabinetmedical.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChiffreAffairesResponse(
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal totalFacture,
        BigDecimal totalEncaisse,
        BigDecimal totalRestant,
        List<ChiffreAffairesTypeResponse> parTypeActe
) {}
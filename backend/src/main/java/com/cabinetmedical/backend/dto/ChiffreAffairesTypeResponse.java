package com.cabinetmedical.backend.dto;

import java.math.BigDecimal;

public record ChiffreAffairesTypeResponse(String typeActe, BigDecimal montant, long nombreActes) {}
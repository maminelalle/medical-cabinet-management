package com.cabinetmedical.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Vente d'une ordonnance a la pharmacie : les medicaments delivres et le paiement, obligatoire,
 * avec sa reference hors especes (Bankily, Masrvi, Sedad...).
 */
public record DispensationRequest(@NotNull Long prescriptionId,
                                  @NotEmpty List<@Valid DispensationLineRequest> lignes,
                                  @NotBlank @Size(max = 50) String moyenPaiement,
                                  @Size(max = 100) String referencePaiement) { }

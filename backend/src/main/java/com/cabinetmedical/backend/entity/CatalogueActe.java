package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "catalogue_actes")
@Getter @Setter @NoArgsConstructor
public class CatalogueActe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(nullable = false, length = 50)
    private String type;
    /** Tarif de consultation propre a une specialite (null = tarif general). */
    @Column(length = 150)
    private String specialite;
    @Column(length = 255)
    private String description;
    @Column(name = "montant_defaut", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantDefaut;
    @Column(nullable = false)
    private boolean actif = true;
}
package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_facture")
@Getter @Setter @NoArgsConstructor
public class LigneFacture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "facture_id")
    private Facture facture;
    @ManyToOne @JoinColumn(name = "catalogue_acte_id")
    private CatalogueActe catalogueActe;
    @Column(nullable = false, length = 150)
    private String libelle;
    @Column(name = "type_acte", nullable = false, length = 50)
    private String typeActe;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;
}
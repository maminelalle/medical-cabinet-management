package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "medicaments")
@Getter @Setter @NoArgsConstructor
public class Medicament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nom;
    @Column(length = 100)
    private String dosage;
    @Column(length = 100)
    private String forme;
    /** Famille therapeutique (antalgiques, antibiotiques...) pour la recherche par le medecin. */
    @Column(length = 100)
    private String famille;
    @Column(name = "stock_actuel", nullable = false)
    private Integer stockActuel;
    @Column(name = "seuil_alerte", nullable = false)
    private Integer seuilAlerte;
    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;
    @Column(name = "prix_achat", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixAchat;
    @Column(name = "prix_vente", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixVente;
    @Column(length = 150)
    private String fournisseur;
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
    @Column(nullable = false)
    private boolean actif = true;
}
package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "paiements")
@Getter @Setter @NoArgsConstructor
public class Paiement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "facture_id")
    private Facture facture;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;
    @Column(name = "date_paiement", nullable = false)
    private Instant datePaiement = Instant.now();
    @Column(name = "moyen_paiement", length = 50)
    private String moyenPaiement;
    @ManyToOne @JoinColumn(name = "enregistre_par")
    private Utilisateur enregistrePar;
}
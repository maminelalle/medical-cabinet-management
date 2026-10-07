package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Soin realise au cabinet (injection, perfusion, pansement...) : le patient vient souvent avec une ordonnance
 * du cabinet ou d'un prescripteur externe et parfois avec son propre produit.
 */
@Entity
@Table(name = "soins")
@Getter @Setter @NoArgsConstructor
public class Soin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id")
    private Patient patient;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TypeSoin type;
    @Column(nullable = false, length = 200)
    private String intitule;
    /** Produit administre (ex. Ceftriaxone 1 g), apporte par le patient ou fourni par la pharmacie. */
    @Column(length = 255)
    private String produit;
    @ManyToOne @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    @Column(name = "prescripteur_externe", length = 200)
    private String prescripteurExterne;
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutSoin statut = StatutSoin.EN_ATTENTE;
    @Column(columnDefinition = "TEXT")
    private String observations;
    private Instant debut;
    private Instant fin;
    @ManyToOne @JoinColumn(name = "realise_par")
    private Utilisateur realisePar;
    @ManyToOne @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter @Setter @NoArgsConstructor
public class RendezVous {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id")
    private Patient patient;
    @ManyToOne(optional = false) @JoinColumn(name = "medecin_id")
    private Medecin medecin;
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;
    @Column(length = 500)
    private String motif;
    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes = 30;
    @Column(name = "debut_consultation")
    private Instant debutConsultation;
    @Column(name = "fin_consultation")
    private Instant finConsultation;
    /** Consultation payee dont ce rendez-vous est le controle (gratuit selon la regle du cabinet). */
    @ManyToOne @JoinColumn(name = "rendez_vous_origine_id")
    private RendezVous rendezVousOrigine;
    @Column(name = "numero_file", nullable = false)
    private Integer numeroFile;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutRendezVous statut = StatutRendezVous.PLANIFIE;
    @ManyToOne @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
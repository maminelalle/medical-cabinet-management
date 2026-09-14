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
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutRendezVous statut = StatutRendezVous.PLANIFIE;
    @ManyToOne @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
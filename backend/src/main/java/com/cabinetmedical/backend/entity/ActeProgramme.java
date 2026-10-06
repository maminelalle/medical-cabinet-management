package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/** Acte programme par un medecin pour un patient (chirurgie, traitement, examen...), puis realise ou annule. */
@Entity
@Table(name = "actes_programmes")
@Getter @Setter @NoArgsConstructor
public class ActeProgramme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id")
    private Patient patient;
    @ManyToOne(optional = false) @JoinColumn(name = "medecin_id")
    private Medecin medecin;
    @ManyToOne @JoinColumn(name = "consultation_id")
    private Consultation consultation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TypeActe type;
    @Column(nullable = false, length = 200)
    private String intitule;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;
    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes = 60;
    @Column(length = 100)
    private String lieu;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutActe statut = StatutActe.PLANIFIE;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private ResultatActe resultat;
    @Column(name = "compte_rendu", columnDefinition = "TEXT")
    private String compteRendu;
    @Column(name = "date_realisation")
    private LocalDateTime dateRealisation;
    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;
    @ManyToOne @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

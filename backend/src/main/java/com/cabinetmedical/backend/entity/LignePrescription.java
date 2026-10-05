package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lignes_prescription")
@Getter @Setter @NoArgsConstructor
public class LignePrescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    @ManyToOne @JoinColumn(name = "medicament_id")
    private Medicament medicamentReference;
    @Column(nullable = false, length = 255)
    private String medicament;
    @Column(length = 255)
    private String posologie;
    @Column(length = 100)
    private String duree;
}
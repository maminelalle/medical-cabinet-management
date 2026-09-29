package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "prescriptions")
@Getter @Setter @NoArgsConstructor
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "consultation_id", unique = true)
    private Consultation consultation;
    @Column(name = "date_prescription", nullable = false)
    private LocalDate datePrescription;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<LignePrescription> lignes = new java.util.ArrayList<>();
}
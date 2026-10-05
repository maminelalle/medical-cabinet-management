package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispensations")
@Getter @Setter @NoArgsConstructor
public class Dispensation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false) @JoinColumn(name = "prescription_id", unique = true)
    private Prescription prescription;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id")
    private Patient patient;
    @ManyToOne(optional = false) @JoinColumn(name = "dispensee_par")
    private Utilisateur dispenseePar;
    @Column(name = "date_dispensation", nullable = false)
    private Instant dateDispensation = Instant.now();
    @OneToMany(mappedBy = "dispensation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDispensation> lignes = new ArrayList<>();
}
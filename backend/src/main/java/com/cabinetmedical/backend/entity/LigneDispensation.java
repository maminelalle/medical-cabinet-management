package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lignes_dispensation")
@Getter @Setter @NoArgsConstructor
public class LigneDispensation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "dispensation_id")
    private Dispensation dispensation;
    @ManyToOne(optional = false) @JoinColumn(name = "medicament_id")
    private Medicament medicament;
    @Column(nullable = false)
    private Integer quantite;
}
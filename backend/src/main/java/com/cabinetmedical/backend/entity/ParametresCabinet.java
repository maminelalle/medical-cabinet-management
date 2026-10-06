package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Coordonnees du cabinet imprimees sur les documents (une seule ligne, id = 1). */
@Entity
@Table(name = "parametres_cabinet")
@Getter @Setter @NoArgsConstructor
public class ParametresCabinet {
    public static final long IDENTIFIANT = 1L;

    @Id
    private Long id = IDENTIFIANT;
    @Column(nullable = false, length = 150)
    private String nom;
    @Column(name = "sous_titre", length = 150)
    private String sousTitre;
    @Column(length = 255)
    private String adresse;
    @Column(length = 50)
    private String telephone;
    @Column(length = 255)
    private String email;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

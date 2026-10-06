package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Trace d'une action effectuee dans l'application (jamais de donnee medicale dans la description). */
@Entity
@Table(name = "journal_activite")
@Getter @Setter @NoArgsConstructor
public class JournalActivite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    @Column(length = 255)
    private String email;
    @Column(length = 20)
    private String role;
    @Column(nullable = false, length = 60)
    private String action;
    @Column(length = 500)
    private String description;
    @Column(length = 10)
    private String methode;
    @Column(length = 255)
    private String chemin;
    @Column(name = "statut_http")
    private Integer statutHttp;
    @Column(name = "adresse_ip", length = 64)
    private String adresseIp;
    @Column(name = "date_action", nullable = false)
    private Instant dateAction = Instant.now();
}

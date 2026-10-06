package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Session de connexion : un jeton JWT emis, l'appareil utilise et l'activite. */
@Entity
@Table(name = "sessions_utilisateur")
@Getter @Setter @NoArgsConstructor
public class SessionUtilisateur {
    public static final String FIN_DECONNEXION = "DECONNEXION";
    public static final String FIN_REVOQUEE = "REVOQUEE";
    public static final String FIN_EXPIREE = "EXPIREE";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    @Column(name = "jeton_id", nullable = false, unique = true, length = 64)
    private String jetonId;
    @Column(name = "adresse_ip", length = 64)
    private String adresseIp;
    @Column(length = 60)
    private String appareil;
    @Column(length = 60)
    private String navigateur;
    @Column(length = 60)
    private String systeme;
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    @Column(name = "date_connexion", nullable = false)
    private Instant dateConnexion = Instant.now();
    @Column(name = "derniere_activite", nullable = false)
    private Instant derniereActivite = Instant.now();
    @Column(name = "date_fin")
    private Instant dateFin;
    @Column(name = "motif_fin", length = 20)
    private String motifFin;
}

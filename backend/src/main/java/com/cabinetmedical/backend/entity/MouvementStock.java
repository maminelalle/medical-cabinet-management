package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Mouvement de stock d'un medicament : quantite signee (+ entree, - sortie) et valeur financiere. */
@Entity
@Table(name = "mouvements_stock")
@Getter @Setter @NoArgsConstructor
public class MouvementStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "medicament_id")
    private Medicament medicament;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TypeMouvement type;
    @Column(nullable = false)
    private Integer quantite;
    @Column(name = "stock_apres", nullable = false)
    private Integer stockApres;
    @Column(name = "prix_unitaire", precision = 12, scale = 2)
    private BigDecimal prixUnitaire;
    @Column(precision = 12, scale = 2)
    private BigDecimal montant;
    @Column(length = 150)
    private String fournisseur;
    @Column(length = 100)
    private String reference;
    @Column(length = 255)
    private String commentaire;
    @ManyToOne @JoinColumn(name = "dispensation_id")
    private Dispensation dispensation;
    @ManyToOne @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    @Column(name = "date_mouvement", nullable = false)
    private Instant dateMouvement = Instant.now();
}

package com.cabinetmedical.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "factures")
@Getter @Setter @NoArgsConstructor
public class Facture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "patient_id")
    private Patient patient;
    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture;
    @Column(name = "montant_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTotal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StatutFacture statut = StatutFacture.EN_ATTENTE;
    @ManyToOne @JoinColumn(name = "created_by")
    private Utilisateur createdBy;
    /** Origine de la facture : un rendez-vous, un acte programme, ou une dispensation (lien porte par Dispensation). */
    @ManyToOne @JoinColumn(name = "rendez_vous_id")
    private RendezVous rendezVous;
    @ManyToOne @JoinColumn(name = "acte_programme_id")
    private ActeProgramme acteProgramme;
    @ManyToOne @JoinColumn(name = "soin_id")
    private Soin soin;
    @Column(name = "motif_annulation", length = 500)
    private String motifAnnulation;
    @Column(name = "date_annulation")
    private Instant dateAnnulation;
    @ManyToOne @JoinColumn(name = "annulee_par")
    private Utilisateur annuleePar;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<LigneFacture> lignes = new java.util.ArrayList<>();
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Paiement> paiements = new java.util.ArrayList<>();
}
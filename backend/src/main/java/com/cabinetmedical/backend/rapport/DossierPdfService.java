package com.cabinetmedical.backend.rapport;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.rapport.RapportPdfService.EnTeteRapport;
import com.cabinetmedical.backend.rapport.RapportPdfService.LigneRapport;
import com.cabinetmedical.backend.service.DossierService;
import com.cabinetmedical.backend.service.ParametresCabinetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Export du dossier patient complet en PDF (JasperReports). Memes droits que la consultation du dossier. */
@Service
@RequiredArgsConstructor
public class DossierPdfService {
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String, String> STATUTS = Map.ofEntries(
            Map.entry("PLANIFIE", "Planifié"), Map.entry("CONFIRME", "Confirmé"), Map.entry("EN_COURS", "En cours"),
            Map.entry("TERMINE", "Terminé"), Map.entry("ANNULE", "Annulé"), Map.entry("ABSENT", "Absent"),
            Map.entry("EN_ATTENTE", "En attente"), Map.entry("PARTIELLE", "Partiellement payée"), Map.entry("PAYEE", "Payée"),
            Map.entry("ANNULEE", "Annulée"), Map.entry("REALISE", "Réalisé"), Map.entry("REUSSI", "Réussi"),
            Map.entry("PARTIEL", "Partiel"), Map.entry("ECHEC", "Échec"));

    private final DossierService dossierService;
    private final RapportPdfService rapportPdfService;
    private final ParametresCabinetService parametresCabinetService;

    @Transactional(readOnly = true)
    public byte[] generer(Long patientId, UserDetails connecte) {
        DossierPatientResponse dossier = dossierService.dossier(patientId, connecte);
        PatientResponse patient = dossier.patient();
        List<LigneRapport> lignes = new ArrayList<>();

        String identite = "1. Identité";
        lignes.add(new LigneRapport(identite, "Dossier n°", String.valueOf(patient.id()), ""));
        lignes.add(new LigneRapport(identite, "Date de naissance", jour(patient.dateNaissance()), ""));
        lignes.add(new LigneRapport(identite, "Téléphone", valeur(patient.telephone()), ""));
        lignes.add(new LigneRapport(identite, "Email", valeur(patient.email()), ""));
        lignes.add(new LigneRapport(identite, "Adresse", valeur(patient.adresse()), ""));

        String consultations = "2. Consultations et ordonnances (" + dossier.consultations().size() + ")";
        if (dossier.consultations().isEmpty()) lignes.add(new LigneRapport(consultations, "Aucune consultation", "", ""));
        for (ConsultationDossierResponse consultation : dossier.consultations()) {
            lignes.add(new LigneRapport(consultations, jourHeure(consultation.dateHeure()),
                    "Dr. " + consultation.medecinPrenom() + " " + consultation.medecinNom() + " — " + valeur(consultation.motif()) + "\n"
                            + (consultation.compteRendu() == null ? "Compte-rendu réservé au médecin (secret médical)" : consultation.compteRendu()),
                    consultation.prescription() == null ? "" : consultation.ordonnanceDelivree() ? "Ordonnance délivrée" : "Ordonnance"));
            if (consultation.prescription() != null) {
                for (LignePrescriptionResponse ligne : consultation.prescription().lignes()) {
                    lignes.add(new LigneRapport(consultations, "   ℞ " + ligne.medicament(),
                            valeur(ligne.posologie()) + " · " + valeur(ligne.duree()), ""));
                }
            }
        }

        String actes = "3. Actes programmés (" + dossier.actes().size() + ")";
        if (dossier.actes().isEmpty()) lignes.add(new LigneRapport(actes, "Aucun acte programmé", "", ""));
        for (ActeProgrammeResponse acte : dossier.actes()) {
            String detail = acte.type() + " · Dr. " + acte.medecinPrenom() + " " + acte.medecinNom()
                    + (acte.details() == null ? "" : "\n" + acte.details())
                    + (acte.compteRendu() == null ? "" : "\nCompte-rendu : " + acte.compteRendu());
            lignes.add(new LigneRapport(actes, jourHeure(acte.dateHeure()) + "\n" + acte.intitule(), detail,
                    statut(acte.statut().name()) + (acte.resultat() == null ? "" : " · " + statut(acte.resultat().name()))));
        }

        String rendezVous = "4. Rendez-vous (" + dossier.rendezVous().size() + ")";
        if (dossier.rendezVous().isEmpty()) lignes.add(new LigneRapport(rendezVous, "Aucun rendez-vous", "", ""));
        for (RendezVousResponse item : dossier.rendezVous()) {
            lignes.add(new LigneRapport(rendezVous, jourHeure(item.dateHeure()),
                    "Dr. " + item.medecinPrenom() + " " + item.medecinNom() + " — " + valeur(item.motif()), statut(item.statut().name())));
        }

        String factures = "5. Factures (" + dossier.factures().size() + ")";
        if (dossier.factures().isEmpty()) lignes.add(new LigneRapport(factures, "Aucune facture", "", ""));
        BigDecimal reste = BigDecimal.ZERO;
        for (FactureResponse facture : dossier.factures()) {
            reste = reste.add(facture.resteAPayer());
            lignes.add(new LigneRapport(factures, String.format("FAC-%03d · %s", facture.id(), jour(facture.dateFacture())),
                    statut(facture.statut().name()) + " · payé " + montant(facture.montantPaye()) + " · reste " + montant(facture.resteAPayer()),
                    montant(facture.montantTotal())));
        }
        if (!dossier.factures().isEmpty()) lignes.add(new LigneRapport(factures, "Reste à payer total", "", montant(reste)));

        var cabinet = parametresCabinetService.lire();
        return rapportPdfService.generer(new EnTeteRapport(cabinet.nom(), cabinet.coordonnees(), "Dossier patient",
                patient.prenom() + " " + patient.nom(),
                "Document confidentiel — données de santé couvertes par le secret médical"), lignes);
    }

    private static String jour(LocalDate date) { return date == null ? "—" : date.format(JOUR); }
    private static String jourHeure(LocalDateTime date) { return date == null ? "—" : date.format(JOUR_HEURE); }
    private static String valeur(String texte) { return texte == null || texte.isBlank() ? "—" : texte; }
    private static String statut(String code) { return STATUTS.getOrDefault(code, code); }
    private static String montant(BigDecimal montant) {
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(montant == null ? BigDecimal.ZERO : montant) + " MRU";
    }
}

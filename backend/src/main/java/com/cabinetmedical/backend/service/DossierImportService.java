package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.DossierImportRequest;
import com.cabinetmedical.backend.dto.DossierImportRequest.ConsultationImport;
import com.cabinetmedical.backend.dto.DossierImportRequest.FactureImport;
import com.cabinetmedical.backend.dto.DossierImportRequest.PatientImport;
import com.cabinetmedical.backend.dto.DossierImportRequest.PrescriptionImport;
import com.cabinetmedical.backend.dto.DossierImportRequest.RendezVousImport;
import com.cabinetmedical.backend.dto.DossierImportResponse;
import com.cabinetmedical.backend.entity.Consultation;
import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.LigneFacture;
import com.cabinetmedical.backend.entity.LignePrescription;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Paiement;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.Prescription;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Import d'un dossier patient exporte au format JSON.
 * Si le patient existe deja (meme nom, prenom et date de naissance), l'historique est fusionne :
 * les rendez-vous, consultations, ordonnances et factures deja presents ne sont pas dupliques.
 */
@Service
@RequiredArgsConstructor
public class DossierImportService {
    private static final String COMPTE_RENDU_NON_TRANSMIS = "Compte-rendu non transmis lors de l'import du dossier.";

    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final FactureRepository factureRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public DossierImportResponse importer(DossierImportRequest request, UserDetails utilisateurConnecte) {
        Utilisateur importateur = utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        Medecin medecinParDefaut = medecinRepository.findByUtilisateurId(importateur.getId())
                .orElseGet(() -> medecinRepository.findAll().stream().min(Comparator.comparing(Medecin::getId)).orElse(null));
        List<Medecin> medecins = medecinRepository.findAll();

        PatientImport donneesPatient = request.patient();
        Patient existant = patientRepository.findFirstByNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissance(
                donneesPatient.nom().trim(), donneesPatient.prenom().trim(), donneesPatient.dateNaissance()).orElse(null);
        Patient patient = existant != null ? completer(existant, donneesPatient) : creerPatient(donneesPatient);

        Bilan bilan = new Bilan();
        for (ConsultationImport consultation : liste(request.consultations())) {
            Medecin medecin = resoudreMedecin(medecins, consultation.medecinNom(), consultation.medecinPrenom(), medecinParDefaut);
            if (medecin == null) { bilan.ignores++; continue; }
            importerConsultation(patient, medecin, consultation, importateur, bilan);
        }
        for (RendezVousImport rendezVous : liste(request.rendezVous())) {
            Medecin medecin = resoudreMedecin(medecins, rendezVous.medecinNom(), rendezVous.medecinPrenom(), medecinParDefaut);
            if (medecin == null) { bilan.ignores++; continue; }
            importerRendezVous(patient, medecin, rendezVous, importateur, bilan);
        }
        List<Facture> facturesExistantes = new ArrayList<>(factureRepository.findByPatientIdOrderByDateFactureDesc(patient.getId()));
        for (FactureImport facture : liste(request.factures())) {
            importerFacture(patient, facture, facturesExistantes, importateur, bilan);
        }

        return new DossierImportResponse(patient.getId(), existant == null, bilan.consultations, bilan.ordonnances,
                bilan.rendezVous, bilan.factures, bilan.ignores);
    }

    private Patient creerPatient(PatientImport donnees) {
        Patient patient = new Patient();
        patient.setNom(donnees.nom().trim());
        patient.setPrenom(donnees.prenom().trim());
        patient.setDateNaissance(donnees.dateNaissance());
        patient.setTelephone(donnees.telephone());
        patient.setEmail(donnees.email());
        patient.setAdresse(donnees.adresse());
        return patientRepository.save(patient);
    }

    /** Complete les coordonnees manquantes d'un patient deja connu sans ecraser les donnees existantes. */
    private Patient completer(Patient patient, PatientImport donnees) {
        if (estVide(patient.getTelephone())) patient.setTelephone(donnees.telephone());
        if (estVide(patient.getEmail())) patient.setEmail(donnees.email());
        if (estVide(patient.getAdresse())) patient.setAdresse(donnees.adresse());
        return patientRepository.save(patient);
    }

    private void importerConsultation(Patient patient, Medecin medecin, ConsultationImport donnees,
                                      Utilisateur importateur, Bilan bilan) {
        RendezVous rendezVous = rendezVousRepository
                .findFirstByPatientIdAndMedecinIdAndDateHeure(patient.getId(), medecin.getId(), donnees.dateHeure())
                .orElseGet(() -> {
                    bilan.rendezVous++;
                    return creerRendezVous(patient, medecin, donnees.dateHeure(), donnees.motif(), StatutRendezVous.TERMINE, importateur);
                });

        Consultation consultation = consultationRepository.findByRendezVousId(rendezVous.getId()).orElse(null);
        if (consultation == null) {
            consultation = new Consultation();
            consultation.setRendezVous(rendezVous);
            consultation.setCompteRendu(estVide(donnees.compteRendu()) ? COMPTE_RENDU_NON_TRANSMIS : donnees.compteRendu());
            consultation = consultationRepository.save(consultation);
            rendezVous.setStatut(StatutRendezVous.TERMINE);
            rendezVousRepository.save(rendezVous);
            bilan.consultations++;
        }

        PrescriptionImport ordonnance = donnees.prescription();
        if (ordonnance == null || liste(ordonnance.lignes()).isEmpty()
                || prescriptionRepository.existsByConsultationId(consultation.getId())) {
            return;
        }
        Prescription prescription = new Prescription();
        prescription.setConsultation(consultation);
        prescription.setDatePrescription(ordonnance.datePrescription() != null
                ? ordonnance.datePrescription() : donnees.dateHeure().toLocalDate());
        prescription.setInstructions(ordonnance.instructions());
        ordonnance.lignes().forEach(ligneImport -> {
            LignePrescription ligne = new LignePrescription();
            ligne.setPrescription(prescription);
            ligne.setMedicament(ligneImport.medicament());
            ligne.setPosologie(ligneImport.posologie());
            ligne.setDuree(ligneImport.duree());
            prescription.getLignes().add(ligne);
        });
        prescriptionRepository.save(prescription);
        bilan.ordonnances++;
    }

    private void importerRendezVous(Patient patient, Medecin medecin, RendezVousImport donnees,
                                    Utilisateur importateur, Bilan bilan) {
        if (rendezVousRepository.findFirstByPatientIdAndMedecinIdAndDateHeure(patient.getId(), medecin.getId(), donnees.dateHeure()).isPresent()) {
            return;
        }
        StatutRendezVous statut = donnees.statut() != null ? donnees.statut() : StatutRendezVous.PLANIFIE;
        boolean creneauOccupe = statut != StatutRendezVous.ANNULE && rendezVousRepository
                .existsByMedecinIdAndDateHeureAndStatutNot(medecin.getId(), donnees.dateHeure(), StatutRendezVous.ANNULE);
        if (creneauOccupe) {
            bilan.ignores++;
            return;
        }
        creerRendezVous(patient, medecin, donnees.dateHeure(), donnees.motif(), statut, importateur);
        bilan.rendezVous++;
    }

    private RendezVous creerRendezVous(Patient patient, Medecin medecin, java.time.LocalDateTime dateHeure, String motif,
                                       StatutRendezVous statut, Utilisateur importateur) {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setPatient(patient);
        rendezVous.setMedecin(medecin);
        rendezVous.setDateHeure(dateHeure);
        rendezVous.setMotif(motif);
        rendezVous.setStatut(statut);
        LocalDate jour = dateHeure.toLocalDate();
        rendezVous.setNumeroFile((int) rendezVousRepository.countByDateHeureGreaterThanEqualAndDateHeureLessThan(
                jour.atStartOfDay(), jour.plusDays(1).atStartOfDay()) + 1);
        rendezVous.setCreatedBy(importateur);
        return rendezVousRepository.save(rendezVous);
    }

    private void importerFacture(Patient patient, FactureImport donnees, List<Facture> existantes,
                                 Utilisateur importateur, Bilan bilan) {
        if (liste(donnees.lignes()).isEmpty()) { bilan.ignores++; return; }
        BigDecimal total = donnees.lignes().stream().map(ligne -> ligne.montant().max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean doublon = existantes.stream().anyMatch(facture -> facture.getDateFacture().equals(donnees.dateFacture())
                && facture.getMontantTotal().compareTo(total) == 0);
        if (doublon) return;

        Facture facture = new Facture();
        facture.setPatient(patient);
        facture.setDateFacture(donnees.dateFacture());
        facture.setMontantTotal(total);
        facture.setCreatedBy(importateur);
        donnees.lignes().forEach(ligneImport -> {
            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(facture);
            ligne.setLibelle(ligneImport.libelle());
            ligne.setTypeActe(estVide(ligneImport.typeActe()) ? "AUTRE" : ligneImport.typeActe().toUpperCase());
            ligne.setMontant(ligneImport.montant().max(BigDecimal.ZERO));
            facture.getLignes().add(ligne);
        });
        BigDecimal paye = BigDecimal.ZERO;
        for (var paiementImport : liste(donnees.paiements())) {
            BigDecimal reste = total.subtract(paye);
            if (paiementImport.montant().signum() <= 0 || reste.signum() <= 0) continue;
            BigDecimal montant = paiementImport.montant().min(reste);
            Paiement paiement = new Paiement();
            paiement.setFacture(facture);
            paiement.setMontant(montant);
            paiement.setDatePaiement(paiementImport.datePaiement() != null ? paiementImport.datePaiement() : Instant.now());
            paiement.setMoyenPaiement(paiementImport.moyenPaiement());
            paiement.setEnregistrePar(importateur);
            facture.getPaiements().add(paiement);
            paye = paye.add(montant);
        }
        if (donnees.statut() == StatutFacture.ANNULEE) facture.setStatut(StatutFacture.ANNULEE);
        else if (paye.signum() == 0) facture.setStatut(StatutFacture.EN_ATTENTE);
        else if (paye.compareTo(total) >= 0) facture.setStatut(StatutFacture.PAYEE);
        else facture.setStatut(StatutFacture.PARTIELLE);
        existantes.add(factureRepository.save(facture));
        bilan.factures++;
    }

    /** Retrouve le medecin par son nom ; a defaut, le medecin importateur ou le premier medecin du cabinet. */
    private Medecin resoudreMedecin(List<Medecin> medecins, String nom, String prenom, Medecin parDefaut) {
        if (!estVide(nom)) {
            return medecins.stream()
                    .filter(medecin -> medecin.getNom().equalsIgnoreCase(nom.trim())
                            && (estVide(prenom) || medecin.getPrenom().equalsIgnoreCase(prenom.trim())))
                    .findFirst().orElse(parDefaut);
        }
        return parDefaut;
    }

    private static <T> List<T> liste(List<T> valeurs) { return valeurs == null ? List.of() : valeurs; }
    private static boolean estVide(String valeur) { return valeur == null || valeur.isBlank(); }

    private static final class Bilan {
        int consultations;
        int ordonnances;
        int rendezVous;
        int factures;
        int ignores;
    }
}

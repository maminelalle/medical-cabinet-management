package com.cabinetmedical.backend;

import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.Role;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de bout en bout des regles metier, a travers l'API reelle (securite JWT comprise) sur une base H2.
 */
@SpringBootTest
class ParcoursMetierIntegrationTest {
    private static final String MOT_DE_PASSE = "password";

    @Autowired private WebApplicationContext contexte;
    @Autowired private PasswordEncoder encodeur;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private MedecinRepository medecinRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private RendezVousRepository rendezVousRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private DispensationRepository dispensationRepository;
    @Autowired private FactureRepository factureRepository;
    @Autowired private PaiementRepository paiementRepository;
    @Autowired private LigneFactureRepository ligneFactureRepository;

    private MockMvc mvc;
    private Medecin medecin;
    private Medecin autreMedecin;
    private Patient patient;

    @BeforeEach
    void preparerDonnees() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
        dispensationRepository.deleteAll();
        prescriptionRepository.deleteAll();
        consultationRepository.deleteAll();
        paiementRepository.deleteAll();
        ligneFactureRepository.deleteAll();
        factureRepository.deleteAll();
        rendezVousRepository.deleteAll();
        patientRepository.deleteAll();
        medecinRepository.deleteAll();
        utilisateurRepository.deleteAll();

        utilisateur("accueil@test.local", Role.ACCUEIL);
        utilisateur("direction@test.local", Role.DIRECTION);
        utilisateur("pharmacien@test.local", Role.PHARMACIEN);
        medecin = medecin("medecin@test.local", "Dupont", "Marie");
        autreMedecin = medecin("cardio@test.local", "Martin", "Paul");

        patient = new Patient();
        patient.setNom("Diallo");
        patient.setPrenom("Aminata");
        patient.setDateNaissance(LocalDate.of(1987, 4, 12));
        patient = patientRepository.save(patient);
    }

    // --- Facturation ---------------------------------------------------------------------------

    @Test
    void lePaiementRecalculeLeStatutEtRefuseUnMontantSuperieurAuResteDu() throws Exception {
        String accueil = connexion("accueil@test.local");
        long factureId = creerFacture(accueil, "600", "400");

        paiement(accueil, factureId, "300").andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PARTIELLE"))
                .andExpect(jsonPath("$.resteAPayer").value(700.0));
        paiement(accueil, factureId, "800").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Le paiement dépasse le reste à payer"));
        paiement(accueil, factureId, "700").andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PAYEE"))
                .andExpect(jsonPath("$.resteAPayer").value(0.0));
    }

    @Test
    void uneFactureSansPaiementPeutEtreAnnuleeAvecUnMotif() throws Exception {
        String accueil = connexion("accueil@test.local");
        long factureId = creerFacture(accueil, "500");

        annulation(accueil, factureId, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motif").exists());
        annulation(accueil, factureId, "Erreur de saisie").andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.motifAnnulation").value("Erreur de saisie"))
                .andExpect(jsonPath("$.resteAPayer").value(0));
        annulation(accueil, factureId, "Encore").andExpect(status().isConflict());
        paiement(accueil, factureId, "100").andExpect(status().isConflict());
    }

    @Test
    void uneFactureDejaEncaisseeNePeutPasEtreAnnulee() throws Exception {
        String accueil = connexion("accueil@test.local");
        long factureId = creerFacture(accueil, "500");
        paiement(accueil, factureId, "100").andExpect(status().isOk());

        annulation(accueil, factureId, "Erreur").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("déjà reçu un paiement")));
    }

    // --- Droits par role -----------------------------------------------------------------------

    @Test
    void chaqueRoleEstLimiteASonPerimetre() throws Exception {
        String medecinJeton = connexion("medecin@test.local");
        String pharmacien = connexion("pharmacien@test.local");
        String direction = connexion("direction@test.local");

        mvc.perform(post("/api/factures").header("Authorization", medecinJeton).contentType(MediaType.APPLICATION_JSON)
                        .content(factureJson("100")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statut").value(403));
        mvc.perform(get("/api/dashboard/impayes").header("Authorization", medecinJeton)).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard/impayes").header("Authorization", direction)).andExpect(status().isOk());
        mvc.perform(get("/api/patients/" + patient.getId() + "/historique").header("Authorization", pharmacien))
                .andExpect(status().isForbidden());
    }

    @Test
    void uneConnexionAvecUnMauvaisMotDePasseEstRefusee() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"accueil@test.local\",\"motDePasse\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void leMedecinNAccedeQuAuxDossiersDeSesPatientsEtLAccueilNeVoitPasLeCompteRendu() throws Exception {
        String accueil = connexion("accueil@test.local");
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), LocalDateTime.now().minusDays(1).withNano(0));
        mvc.perform(post("/api/rendezvous/" + rendezVousId + "/consultation").header("Authorization", connexion("medecin@test.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"compteRendu\":\"Examen normal\"}"))
                .andExpect(status().isCreated());

        String chemin = "/api/patients/" + patient.getId() + "/historique";
        mvc.perform(get(chemin).header("Authorization", connexion("medecin@test.local"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.consultations[0].compteRendu").value("Examen normal"));
        mvc.perform(get(chemin).header("Authorization", connexion("cardio@test.local"))).andExpect(status().isForbidden());
        mvc.perform(get(chemin).header("Authorization", accueil)).andExpect(status().isOk())
                .andExpect(jsonPath("$.compteRenduMasque").value(true))
                .andExpect(jsonPath("$.consultations[0].compteRendu").doesNotExist());
    }

    // --- Patients et rendez-vous ---------------------------------------------------------------

    @Test
    void unPatientAvecHistoriqueNePeutPasEtreSupprime() throws Exception {
        String accueil = connexion("accueil@test.local");
        creerFacture(accueil, "100");
        mvc.perform(delete("/api/patients/" + patient.getId()).header("Authorization", accueil))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("historique")));

        Patient sansHistorique = new Patient();
        sansHistorique.setNom("Sans");
        sansHistorique.setPrenom("Historique");
        sansHistorique.setDateNaissance(LocalDate.of(2000, 1, 1));
        long id = patientRepository.save(sansHistorique).getId();
        mvc.perform(delete("/api/patients/" + id).header("Authorization", accueil)).andExpect(status().isNoContent());
    }

    @Test
    void seulUnRendezVousPlanifieOuConfirmePeutEtreModifie() throws Exception {
        String accueil = connexion("accueil@test.local");
        LocalDateTime creneau = LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0);
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), creneau);
        long autreId = creerRendezVous(accueil, medecin.getId(), creneau.plusHours(1));

        modifierRendezVous(accueil, rendezVousId, autreMedecin.getId(), creneau.plusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.medecinId").value(autreMedecin.getId()));
        modifierRendezVous(accueil, autreId, autreMedecin.getId(), creneau.plusDays(1)).andExpect(status().isConflict());

        mvc.perform(patch("/api/rendezvous/" + autreId + "/statut").header("Authorization", accueil)
                .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"ANNULE\"}")).andExpect(status().isOk());
        modifierRendezVous(accueil, autreId, medecin.getId(), creneau.plusDays(2)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("planifié ou confirmé")));
    }

    // --- Import de dossier -----------------------------------------------------------------------

    @Test
    void lImportDUnDossierCreeLePatientPuisFusionneSansDoublon() throws Exception {
        String accueil = connexion("accueil@test.local");
        String dossier = """
                {"format":"cabinet-medical.dossier-patient","version":1,
                 "patient":{"id":99,"nom":"Sow","prenom":"Moussa","dateNaissance":"1990-05-02","telephone":"22 00 00 00"},
                 "consultations":[{"dateHeure":"2026-01-10T10:00:00","motif":"Fièvre","medecinNom":"Dupont","medecinPrenom":"Marie",
                   "compteRendu":"Angine","prescription":{"datePrescription":"2026-01-10","lignes":[{"medicament":"Amoxicilline 500 mg","posologie":"3/j","duree":"7 jours"}]}}],
                 "rendezVous":[{"dateHeure":"2026-01-10T10:00:00","medecinNom":"Dupont","medecinPrenom":"Marie","statut":"TERMINE"}],
                 "factures":[{"dateFacture":"2026-01-10","statut":"PAYEE","lignes":[{"libelle":"Consultation","typeActe":"CONSULTATION","montant":1500}],
                   "paiements":[{"montant":1500,"datePaiement":"2026-01-10T10:30:00Z","moyenPaiement":"ESPECES"}]}]}
                """;

        importer(accueil, dossier).andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCree").value(true))
                .andExpect(jsonPath("$.consultationsImportees").value(1))
                .andExpect(jsonPath("$.ordonnancesImportees").value(1))
                .andExpect(jsonPath("$.facturesImportees").value(1));
        importer(accueil, dossier).andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCree").value(false))
                .andExpect(jsonPath("$.consultationsImportees").value(0))
                .andExpect(jsonPath("$.rendezVousImportes").value(0))
                .andExpect(jsonPath("$.facturesImportees").value(0));
        importer(accueil, "{\"patient\":{\"nom\":\"\"}}").andExpect(status().isBadRequest());
    }

    // --- Outils ------------------------------------------------------------------------------

    private void utilisateur(String email, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(encodeur.encode(MOT_DE_PASSE));
        utilisateur.setRole(role);
        utilisateurRepository.save(utilisateur);
    }

    private Medecin medecin(String email, String nom, String prenom) {
        utilisateur(email, Role.MEDECIN);
        Medecin nouveau = new Medecin();
        nouveau.setUtilisateur(utilisateurRepository.findByEmailIgnoreCase(email).orElseThrow());
        nouveau.setNom(nom);
        nouveau.setPrenom(prenom);
        return medecinRepository.save(nouveau);
    }

    private String connexion(String email) throws Exception {
        String reponse = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + MOT_DE_PASSE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(reponse, "$.token");
    }

    private String factureJson(String... montants) {
        StringBuilder lignes = new StringBuilder();
        for (String montant : montants) {
            if (!lignes.isEmpty()) lignes.append(',');
            lignes.append("{\"libelle\":\"Acte\",\"typeActe\":\"CONSULTATION\",\"montant\":").append(montant).append('}');
        }
        return "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now() + "\",\"lignes\":[" + lignes + "]}";
    }

    private long creerFacture(String jeton, String... montants) throws Exception {
        String reponse = mvc.perform(post("/api/factures").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON).content(factureJson(montants)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(reponse, "$.id")).longValue();
    }

    private ResultActions paiement(String jeton, long factureId, String montant) throws Exception {
        return mvc.perform(post("/api/factures/" + factureId + "/paiements").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":" + montant + ",\"moyenPaiement\":\"ESPECES\"}"));
    }

    private ResultActions annulation(String jeton, long factureId, String motif) throws Exception {
        return mvc.perform(post("/api/factures/" + factureId + "/annulation").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motif\":\"" + motif + "\"}"));
    }

    private long creerRendezVous(String jeton, long medecinId, LocalDateTime dateHeure) throws Exception {
        String reponse = mvc.perform(post("/api/rendezvous").header("Authorization", jeton).contentType(MediaType.APPLICATION_JSON)
                        .content(rendezVousJson(medecinId, dateHeure)))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(reponse, "$.id")).longValue();
    }

    private ResultActions modifierRendezVous(String jeton, long id, long medecinId, LocalDateTime dateHeure) throws Exception {
        return mvc.perform(put("/api/rendezvous/" + id).header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(rendezVousJson(medecinId, dateHeure)));
    }

    private String rendezVousJson(long medecinId, LocalDateTime dateHeure) {
        return "{\"patientId\":" + patient.getId() + ",\"medecinId\":" + medecinId
                + ",\"dateHeure\":\"" + dateHeure + "\",\"motif\":\"Contrôle\"}";
    }

    private ResultActions importer(String jeton, String contenu) throws Exception {
        return mvc.perform(post("/api/patients/import").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON).content(contenu));
    }
}

package com.cabinetmedical.backend;

import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.entity.Role;
import com.cabinetmedical.backend.entity.Utilisateur;
import com.cabinetmedical.backend.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests d'integration : API reelle (securite JWT comprise) sur une base H2 videe avant chaque test,
 * avec un compte par role, deux medecins et un patient.
 */
@SpringBootTest
abstract class IntegrationTestBase {
    protected static final String MOT_DE_PASSE = "password";

    @Autowired protected WebApplicationContext contexte;
    @Autowired protected PasswordEncoder encodeur;
    @Autowired protected UtilisateurRepository utilisateurRepository;
    @Autowired protected MedecinRepository medecinRepository;
    @Autowired protected PatientRepository patientRepository;
    @Autowired protected RendezVousRepository rendezVousRepository;
    @Autowired protected ConsultationRepository consultationRepository;
    @Autowired protected PrescriptionRepository prescriptionRepository;
    @Autowired protected DispensationRepository dispensationRepository;
    @Autowired protected FactureRepository factureRepository;
    @Autowired protected PaiementRepository paiementRepository;
    @Autowired protected LigneFactureRepository ligneFactureRepository;
    @Autowired protected ActeProgrammeRepository acteProgrammeRepository;
    @Autowired protected MouvementStockRepository mouvementStockRepository;
    @Autowired protected MedicamentRepository medicamentRepository;
    @Autowired protected SessionUtilisateurRepository sessionRepository;
    @Autowired protected JournalActiviteRepository journalRepository;
    @Autowired protected SoinRepository soinRepository;
    @Autowired protected CatalogueActeRepository catalogueRepository;
    @Autowired protected ParametresCabinetRepository parametresRepository;

    protected MockMvc mvc;
    protected Medecin medecin;
    protected Medecin autreMedecin;
    protected Patient patient;

    @BeforeEach
    void preparerDonnees() {
        mvc = MockMvcBuilders.webAppContextSetup(contexte).apply(springSecurity()).build();
        journalRepository.deleteAll();
        sessionRepository.deleteAll();
        mouvementStockRepository.deleteAll();
        dispensationRepository.deleteAll();
        paiementRepository.deleteAll();
        ligneFactureRepository.deleteAll();
        factureRepository.deleteAll();
        acteProgrammeRepository.deleteAll();
        soinRepository.deleteAll();
        catalogueRepository.deleteAll();
        parametresRepository.deleteAll();
        prescriptionRepository.deleteAll();
        consultationRepository.deleteAll();
        // Les controles referencent leur consultation d origine : ils partent en premier.
        rendezVousRepository.deleteAll(rendezVousRepository.findAll().stream().filter(r -> r.getRendezVousOrigine() != null).toList());
        rendezVousRepository.deleteAll();
        patientRepository.deleteAll();
        medecinRepository.deleteAll();
        medicamentRepository.deleteAll();
        utilisateurRepository.deleteAll();

        utilisateur("accueil@test.local", Role.ACCUEIL);
        utilisateur("direction@test.local", Role.DIRECTION);
        utilisateur("pharmacien@test.local", Role.PHARMACIEN);
        utilisateur("admin@test.local", Role.ADMIN);
        medecin = medecin("medecin@test.local", "Ould Cheikh", "Mohamed");
        autreMedecin = medecin("cardio@test.local", "Mint Ahmed", "Zeinabou");
        patient = nouveauPatient("Ould Mohamed", "Lalle");
    }

    protected Patient nouveauPatient(String nom, String prenom) {
        Patient nouveau = new Patient();
        nouveau.setNom(nom);
        nouveau.setPrenom(prenom);
        nouveau.setDateNaissance(LocalDate.of(1987, 4, 12));
        return patientRepository.save(nouveau);
    }

    protected Utilisateur utilisateur(String email, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(encodeur.encode(MOT_DE_PASSE));
        utilisateur.setRole(role);
        utilisateur.setNom(role.name());
        utilisateur.setPrenom("Test");
        return utilisateurRepository.save(utilisateur);
    }

    protected Medecin medecin(String email, String nom, String prenom) {
        Medecin nouveau = new Medecin();
        nouveau.setUtilisateur(utilisateur(email, Role.MEDECIN));
        nouveau.setNom(nom);
        nouveau.setPrenom(prenom);
        return medecinRepository.save(nouveau);
    }

    protected String connexion(String email) throws Exception {
        String reponse = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"" + MOT_DE_PASSE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(reponse, "$.token");
    }

    protected ResultActions envoyer(String methode, String url, String jeton, String json) throws Exception {
        var requete = switch (methode) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            case "PATCH" -> patch(url);
            case "DELETE" -> delete(url);
            default -> get(url);
        };
        requete.header("Authorization", jeton);
        if (json != null) requete.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(requete);
    }

    protected static long id(ResultActions resultat) throws Exception {
        return ((Number) JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    protected String factureJson(String... montants) {
        StringBuilder lignes = new StringBuilder();
        for (String montant : montants) {
            if (!lignes.isEmpty()) lignes.append(',');
            lignes.append("{\"libelle\":\"Acte\",\"typeActe\":\"CONSULTATION\",\"montant\":").append(montant).append('}');
        }
        return "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now() + "\",\"lignes\":[" + lignes + "]}";
    }

    protected long creerFacture(String jeton, String... montants) throws Exception {
        return id(envoyer("POST", "/api/factures", jeton, factureJson(montants))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statut").value("EN_ATTENTE")));
    }

    protected ResultActions paiement(String jeton, long factureId, String montant) throws Exception {
        return envoyer("POST", "/api/factures/" + factureId + "/paiements", jeton,
                "{\"montant\":" + montant + ",\"moyenPaiement\":\"ESPECES\"}");
    }

    protected String rendezVousJson(long patientId, long medecinId, LocalDateTime dateHeure) {
        return "{\"patientId\":" + patientId + ",\"medecinId\":" + medecinId
                + ",\"dateHeure\":\"" + dateHeure + "\",\"dureeMinutes\":30,\"motif\":\"Contrôle\"}";
    }

    protected long creerRendezVous(String jeton, long medecinId, LocalDateTime dateHeure) throws Exception {
        return id(envoyer("POST", "/api/rendezvous", jeton, rendezVousJson(patient.getId(), medecinId, dateHeure))
                .andExpect(status().isCreated()));
    }

    /** Demarre puis termine une consultation (compte-rendu) ; renvoie l'identifiant de la consultation. */
    protected long consulter(String jetonMedecin, long rendezVousId, String compteRendu) throws Exception {
        envoyer("PATCH", "/api/rendezvous/" + rendezVousId + "/statut", jetonMedecin, "{\"statut\":\"EN_COURS\"}")
                .andExpect(status().isOk());
        return id(envoyer("POST", "/api/rendezvous/" + rendezVousId + "/consultation", jetonMedecin,
                "{\"compteRendu\":\"" + compteRendu + "\"}").andExpect(status().isCreated()));
    }

    /** Un creneau futur a heure fixe, a {@code jours} jours d'ici. */
    protected static LocalDateTime creneau(int jours, int heure, int minute) {
        return LocalDate.now().plusDays(jours).atTime(heure, minute);
    }
}

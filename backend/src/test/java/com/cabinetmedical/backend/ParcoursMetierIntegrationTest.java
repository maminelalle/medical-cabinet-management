package com.cabinetmedical.backend;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regles metier du coeur : facturation, droits, dossier, patients, rendez-vous, import. */
class ParcoursMetierIntegrationTest extends IntegrationTestBase {

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
        envoyer("POST", "/api/factures", medecinJeton, factureJson("100"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.statut").value(403));
        envoyer("GET", "/api/dashboard/impayes", medecinJeton, null).andExpect(status().isForbidden());
        envoyer("GET", "/api/dashboard/impayes", connexion("direction@test.local"), null).andExpect(status().isOk());
        envoyer("GET", "/api/patients/" + patient.getId() + "/historique", connexion("pharmacien@test.local"), null)
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
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        consulter(connexion("medecin@test.local"), rendezVousId, "Examen normal");

        String chemin = "/api/patients/" + patient.getId() + "/historique";
        envoyer("GET", chemin, connexion("medecin@test.local"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.consultations[0].compteRendu").value("Examen normal"));
        envoyer("GET", chemin, connexion("cardio@test.local"), null).andExpect(status().isForbidden());
        envoyer("GET", chemin, accueil, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.compteRenduMasque").value(true))
                .andExpect(jsonPath("$.consultations[0].compteRendu").doesNotExist());
    }

    // --- Patients et rendez-vous ---------------------------------------------------------------

    @Test
    void unPatientAvecHistoriqueNePeutPasEtreSupprime() throws Exception {
        String accueil = connexion("accueil@test.local");
        creerFacture(accueil, "100");
        envoyer("DELETE", "/api/patients/" + patient.getId(), accueil, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("historique")));

        long id = nouveauPatient("Sans", "Historique").getId();
        envoyer("DELETE", "/api/patients/" + id, accueil, null).andExpect(status().isNoContent());
    }

    @Test
    void seulUnRendezVousPlanifieOuConfirmePeutEtreModifie() throws Exception {
        String accueil = connexion("accueil@test.local");
        LocalDateTime debut = creneau(3, 9, 0);
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), debut);
        long autreId = creerRendezVous(accueil, medecin.getId(), debut.plusHours(1));

        modifier(accueil, rendezVousId, autreMedecin.getId(), debut.plusDays(1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.medecinId").value(autreMedecin.getId()));
        modifier(accueil, autreId, autreMedecin.getId(), debut.plusDays(1)).andExpect(status().isConflict());

        envoyer("PATCH", "/api/rendezvous/" + autreId + "/statut", accueil, "{\"statut\":\"ANNULE\"}").andExpect(status().isOk());
        modifier(accueil, autreId, medecin.getId(), debut.plusDays(2)).andExpect(status().isConflict())
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

        envoyer("POST", "/api/patients/import", accueil, dossier).andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCree").value(true))
                .andExpect(jsonPath("$.consultationsImportees").value(1))
                .andExpect(jsonPath("$.ordonnancesImportees").value(1))
                .andExpect(jsonPath("$.facturesImportees").value(1));
        envoyer("POST", "/api/patients/import", accueil, dossier).andExpect(status().isOk())
                .andExpect(jsonPath("$.patientCree").value(false))
                .andExpect(jsonPath("$.consultationsImportees").value(0))
                .andExpect(jsonPath("$.rendezVousImportes").value(0))
                .andExpect(jsonPath("$.facturesImportees").value(0));
        envoyer("POST", "/api/patients/import", accueil, "{\"patient\":{\"nom\":\"\"}}").andExpect(status().isBadRequest());
    }

    @Test
    void leDossierSExporteEnPdf() throws Exception {
        String accueil = connexion("accueil@test.local");
        creerFacture(accueil, "1500");
        var reponse = envoyer("GET", "/api/patients/" + patient.getId() + "/dossier.pdf", accueil, null)
                .andExpect(status().isOk()).andReturn().getResponse();
        org.assertj.core.api.Assertions.assertThat(reponse.getContentType()).isEqualTo("application/pdf");
        org.assertj.core.api.Assertions.assertThat(new String(reponse.getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
    }

    private org.springframework.test.web.servlet.ResultActions annulation(String jeton, long factureId, String motif) throws Exception {
        return envoyer("POST", "/api/factures/" + factureId + "/annulation", jeton, "{\"motif\":\"" + motif + "\"}");
    }

    private org.springframework.test.web.servlet.ResultActions modifier(String jeton, long id, long medecinId, LocalDateTime dateHeure) throws Exception {
        return envoyer("PUT", "/api/rendezvous/" + id, jeton, rendezVousJson(patient.getId(), medecinId, dateHeure));
    }

}

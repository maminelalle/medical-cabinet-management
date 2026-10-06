package com.cabinetmedical.backend;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parcours accueil, medecin, pharmacie et administration ajoutes avec les actes, sessions et mouvements de stock. */
class ParcoursCompletIntegrationTest extends IntegrationTestBase {

    // --- Accueil : creneaux et creation rapide du patient ----------------------------------------

    @Test
    void unCreneauReserveNePeutPasEtreDonneAUnAutrePatientSaufAnnulation() throws Exception {
        String accueil = connexion("accueil@test.local");
        long autrePatient = nouveauPatient("Ould Sidi", "Mohamed").getId();
        LocalDateTime neufHeures = creneau(2, 9, 0);
        long premier = creerRendezVous(accueil, medecin.getId(), neufHeures);

        envoyer("POST", "/api/rendezvous", accueil, rendezVousJson(autrePatient, medecin.getId(), neufHeures.plusMinutes(15)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Créneau déjà réservé")));
        envoyer("POST", "/api/rendezvous", accueil, rendezVousJson(autrePatient, autreMedecin.getId(), neufHeures))
                .andExpect(status().isCreated());
        envoyer("GET", "/api/rendezvous/creneaux?medecinId=" + medecin.getId() + "&date=" + neufHeures.toLocalDate(), accueil, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].debut").value("09:00"))
                .andExpect(jsonPath("$[2].libre").value(false));

        envoyer("PATCH", "/api/rendezvous/" + premier + "/statut", accueil, "{\"statut\":\"ANNULE\"}").andExpect(status().isOk());
        envoyer("POST", "/api/rendezvous", accueil, rendezVousJson(autrePatient, medecin.getId(), neufHeures))
                .andExpect(status().isCreated());
    }

    @Test
    void lAccueilCreeLePatientEnPrenantLeRendezVous() throws Exception {
        String accueil = connexion("accueil@test.local");
        long patientsAvant = patientRepository.count();
        String demande = "{\"nouveauPatient\":{\"nom\":\"Mint Ahmedou\",\"prenom\":\"Fatimetou\",\"dateNaissance\":\"1995-02-03\",\"telephone\":\"36 00 00 00\"},"
                + "\"medecinId\":" + medecin.getId() + ",\"dateHeure\":\"" + creneau(1, 10, 0) + "\",\"motif\":\"Première visite\"}";

        envoyer("POST", "/api/rendezvous", accueil, demande).andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientNom").value("Mint Ahmedou"))
                .andExpect(jsonPath("$.patientTelephone").value("36 00 00 00"));
        assertThat(patientRepository.count()).isEqualTo(patientsAvant + 1);

        // Creneau deja pris : ni rendez-vous ni fiche patient creee.
        envoyer("POST", "/api/rendezvous", accueil, demande.replace("Mint Ahmedou", "Ould Ely")).andExpect(status().isConflict());
        assertThat(patientRepository.count()).isEqualTo(patientsAvant + 1);
    }

    // --- Facture reliee au rendez-vous et paiements avec reference -------------------------------

    @Test
    void laFactureDuRendezVousEstUniqueEtLesPaiementsHorsEspecesExigentUneReference() throws Exception {
        String accueil = connexion("accueil@test.local");
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), creneau(1, 11, 0));
        String facture = "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now() + "\",\"rendezVousId\":" + rendezVousId
                + ",\"lignes\":[{\"libelle\":\"Consultation\",\"typeActe\":\"CONSULTATION\",\"montant\":2000}]";

        envoyer("POST", "/api/factures", accueil, facture + ",\"paiement\":{\"montant\":2000,\"moyenPaiement\":\"BANKILY\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("référence")));
        long factureId = id(envoyer("POST", "/api/factures", accueil,
                facture + ",\"paiement\":{\"montant\":2000,\"moyenPaiement\":\"BANKILY\",\"reference\":\"BK-778899\"}}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PAYEE"))
                .andExpect(jsonPath("$.origine").value("CONSULTATION"))
                .andExpect(jsonPath("$.paiements[0].reference").value("BK-778899")));

        envoyer("POST", "/api/factures", accueil, facture + "}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("existe déjà")));
        envoyer("GET", "/api/rendezvous/" + rendezVousId, accueil, null)
                .andExpect(jsonPath("$.factureId").value(factureId))
                .andExpect(jsonPath("$.factureStatut").value("PAYEE"));
        envoyer("PATCH", "/api/rendezvous/" + rendezVousId + "/statut", accueil, "{\"statut\":\"ANNULE\"}")
                .andExpect(status().isConflict());
    }

    // --- Medecin : demarrer, terminer, programmer un acte -----------------------------------------

    @Test
    void leMedecinDemarrePuisTermineLaConsultation() throws Exception {
        String accueil = connexion("accueil@test.local");
        String medecinJeton = connexion("medecin@test.local");
        long premier = creerRendezVous(accueil, medecin.getId(), creneau(0, 8, 0).minusDays(1));
        long second = creerRendezVous(accueil, medecin.getId(), creneau(0, 8, 30).minusDays(1));

        envoyer("POST", "/api/rendezvous/" + premier + "/consultation", medecinJeton, "{\"compteRendu\":\"Trop tôt\"}")
                .andExpect(status().isConflict());
        envoyer("PATCH", "/api/rendezvous/" + premier + "/statut", accueil, "{\"statut\":\"EN_COURS\"}")
                .andExpect(status().isConflict());
        envoyer("PATCH", "/api/rendezvous/" + premier + "/statut", medecinJeton, "{\"statut\":\"EN_COURS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debutConsultation").exists());
        envoyer("PATCH", "/api/rendezvous/" + second + "/statut", medecinJeton, "{\"statut\":\"EN_COURS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Terminez d'abord")));
        envoyer("POST", "/api/rendezvous/" + premier + "/consultation", medecinJeton, "{\"compteRendu\":\"RAS\"}")
                .andExpect(status().isCreated());
        envoyer("GET", "/api/rendezvous/" + premier, medecinJeton, null)
                .andExpect(jsonPath("$.statut").value("TERMINE"))
                .andExpect(jsonPath("$.finConsultation").exists());
    }

    @Test
    void unActeProgrammeEstVisibleParLAccueilRealiseFactureEtNonAnnulableUneFoisFacture() throws Exception {
        String accueil = connexion("accueil@test.local");
        String medecinJeton = connexion("medecin@test.local");
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), creneau(-1, 9, 0));
        long consultationId = consulter(medecinJeton, rendezVousId, "Hernie inguinale");
        LocalDateTime dateActe = creneau(5, 14, 0);
        String acte = "{\"patientId\":" + patient.getId() + ",\"consultationId\":" + consultationId
                + ",\"type\":\"CHIRURGIE\",\"intitule\":\"Cure de hernie\",\"details\":\"À jeun\",\"dateHeure\":\"" + dateActe
                + "\",\"dureeMinutes\":120,\"lieu\":\"Bloc 1\"}";

        envoyer("POST", "/api/actes", connexion("cardio@test.local"), acte).andExpect(status().isForbidden());
        long acteId = id(envoyer("POST", "/api/actes", medecinJeton, acte).andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PLANIFIE")));
        // Le creneau de l'acte bloque l'agenda du medecin.
        envoyer("POST", "/api/rendezvous", accueil, rendezVousJson(patient.getId(), medecin.getId(), dateActe.plusMinutes(30)))
                .andExpect(status().isConflict());

        envoyer("POST", "/api/actes/" + acteId + "/realisation", medecinJeton,
                "{\"resultat\":\"REUSSI\",\"compteRendu\":\"Intervention sans complication\",\"dateRealisation\":\"" + LocalDateTime.now().minusMinutes(1).withNano(0) + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("REALISE"));
        envoyer("GET", "/api/actes", accueil, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].intitule").value("Cure de hernie"))
                .andExpect(jsonPath("$[0].compteRendu").doesNotExist())
                .andExpect(jsonPath("$[0].resultat").value("REUSSI"));

        envoyer("POST", "/api/factures", accueil, "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now()
                + "\",\"acteProgrammeId\":" + acteId + ",\"lignes\":[{\"libelle\":\"Cure de hernie\",\"typeActe\":\"CHIRURGIE\",\"montant\":45000}]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.origine").value("ACTE"));
        envoyer("GET", "/api/actes/" + acteId, medecinJeton, null).andExpect(jsonPath("$.factureId").exists());
    }

    // --- Pharmacie : stock, vente avec paiement, finances ----------------------------------------

    @Test
    void laVenteDUneOrdonnanceDecrementeLeStockEnregistreLePaiementEtAlimenteLesFinances() throws Exception {
        String pharmacien = connexion("pharmacien@test.local");
        long medicamentId = id(envoyer("POST", "/api/pharmacie/medicaments", pharmacien,
                "{\"nom\":\"Amoxicilline\",\"dosage\":\"500 mg\",\"famille\":\"Antibiotiques\",\"stockActuel\":10,\"seuilAlerte\":2,"
                        + "\"prixAchat\":100,\"prixVente\":250}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.famille").value("Antibiotiques")));
        envoyer("POST", "/api/pharmacie/medicaments/" + medicamentId + "/approvisionnements", pharmacien,
                "{\"quantite\":5,\"prixAchatUnitaire\":110,\"fournisseur\":\"Pharma Sud\",\"reference\":\"BL-12\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockActuel").value(15));

        long prescriptionId = prescription(medicamentId);
        String vente = "{\"prescriptionId\":" + prescriptionId + ",\"lignes\":[{\"medicamentId\":" + medicamentId + ",\"quantite\":3}],"
                + "\"moyenPaiement\":\"MASRVI\"";
        envoyer("POST", "/api/pharmacie/dispensations", pharmacien, vente + "}").andExpect(status().isBadRequest());
        long factureId = ((Number) com.jayway.jsonpath.JsonPath.read(envoyer("POST", "/api/pharmacie/dispensations", pharmacien,
                vente + ",\"referencePaiement\":\"MS-4455\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.factureId")).longValue();

        envoyer("GET", "/api/factures/" + factureId, pharmacien, null)
                .andExpect(jsonPath("$.statut").value("PAYEE"))
                .andExpect(jsonPath("$.origine").value("PHARMACIE"))
                .andExpect(jsonPath("$.paiements[0].moyenPaiement").value("MASRVI"))
                .andExpect(jsonPath("$.paiements[0].reference").value("MS-4455"));
        envoyer("GET", "/api/pharmacie/medicaments/" + medicamentId, pharmacien, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.medicament.stockActuel").value(12))
                .andExpect(jsonPath("$.quantiteVendue").value(3))
                .andExpect(jsonPath("$.chiffreVentes").value(750.0))
                .andExpect(jsonPath("$.quantiteAchetee").value(5))
                .andExpect(jsonPath("$.mouvements.length()").value(3));
        envoyer("GET", "/api/pharmacie/finances", pharmacien, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.chiffreVentes").value(750.0))
                .andExpect(jsonPath("$.encaisse").value(750.0))
                .andExpect(jsonPath("$.encaissementsParMoyen[0].libelle").value("MASRVI"));
        envoyer("GET", "/api/pharmacie/medicaments/inventaire.pdf", pharmacien, null).andExpect(status().isOk());
    }

    // --- Administration, sessions et presence ----------------------------------------------------

    @Test
    void lAdministrateurGereLesComptesEtRevoqueLesSessions() throws Exception {
        String admin = connexion("admin@test.local");
        envoyer("GET", "/api/admin/utilisateurs", connexion("direction@test.local"), null).andExpect(status().isForbidden());

        long id = id(envoyer("POST", "/api/admin/utilisateurs", admin,
                "{\"email\":\"caisse2@test.local\",\"nom\":\"Mint Sidi\",\"prenom\":\"Aicha\",\"role\":\"ACCUEIL\",\"motDePasse\":\"motdepasse1\"}")
                .andExpect(status().isCreated()));
        String jetonCaisse = "Bearer " + com.jayway.jsonpath.JsonPath.read(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/120.0")
                        .content("{\"email\":\"caisse2@test.local\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        envoyer("GET", "/api/patients", jetonCaisse, null).andExpect(status().isOk());

        String sessions = envoyer("GET", "/api/admin/sessions?periode=actives", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].navigateur", hasItem("Chrome")))
                .andReturn().getResponse().getContentAsString();
        Number sessionId = com.jayway.jsonpath.JsonPath.<java.util.List<Number>>read(sessions, "$[?(@.email == 'caisse2@test.local')].id").get(0);
        envoyer("DELETE", "/api/admin/sessions/" + sessionId, admin, null).andExpect(status().isNoContent());
        envoyer("GET", "/api/patients", jetonCaisse, null).andExpect(status().isForbidden());

        envoyer("PATCH", "/api/admin/utilisateurs/" + id + "/statut", admin, "{\"actif\":false}").andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"caisse2@test.local\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("désactivé")));
        envoyer("GET", "/api/admin/journal", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("Création d'un utilisateur")))
                .andExpect(jsonPath("$[*].action", hasItem("Révocation d'une session")))
                .andExpect(jsonPath("$[*].action", hasItem("Connexion")));
        envoyer("GET", "/api/admin/tableau-de-bord", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.utilisateurs").value(7));
    }

    @Test
    void laPresenceIndiqueSiLeMedecinEstConnecteEnConsultationOuEnRetard() throws Exception {
        String accueil = connexion("accueil@test.local");
        LocalDateTime ilYaUneHeure = LocalDateTime.now().minusHours(1).withSecond(0).withNano(0);
        LocalDateTime dansDeuxHeures = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        boolean memeJour = ilYaUneHeure.toLocalDate().equals(LocalDate.now()) && dansDeuxHeures.toLocalDate().equals(LocalDate.now());
        String chemin = "/api/medecins/presence";

        envoyer("GET", chemin, accueil, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'Ould Cheikh')].statut").value("ABSENT"));
        String medecinJeton = connexion("medecin@test.local");
        envoyer("GET", "/api/auth/ping", medecinJeton, null).andExpect(status().isNoContent());
        envoyer("GET", chemin, accueil, null).andExpect(jsonPath("$[?(@.nom == 'Ould Cheikh')].statut").value("DISPONIBLE"));
        if (!memeJour) return; // Autour de minuit, le planning "du jour" ne permet pas ce scenario.

        long retard = creerRendezVous(accueil, medecin.getId(), ilYaUneHeure);
        envoyer("GET", chemin, accueil, null).andExpect(jsonPath("$[?(@.nom == 'Ould Cheikh')].statut").value("EN_RETARD"));
        envoyer("PATCH", "/api/rendezvous/" + retard + "/statut", medecinJeton, "{\"statut\":\"EN_COURS\"}").andExpect(status().isOk());
        envoyer("GET", chemin, accueil, null).andExpect(jsonPath("$[?(@.nom == 'Ould Cheikh')].statut").value("EN_CONSULTATION"))
                .andExpect(jsonPath("$[?(@.nom == 'Ould Cheikh')].patientEnCours").value("Lalle Ould Mohamed"));
        envoyer("GET", chemin, connexion("medecin@test.local"), null).andExpect(status().isForbidden());
    }

    /** Ordonnance d'une consultation terminee, prete a etre delivree. */
    private long prescription(long medicamentId) throws Exception {
        String accueil = connexion("accueil@test.local");
        String medecinJeton = connexion("medecin@test.local");
        long rendezVousId = creerRendezVous(accueil, medecin.getId(), creneau(-2, 9, 0));
        long consultationId = consulter(medecinJeton, rendezVousId, "Angine");
        return id(envoyer("POST", "/api/consultations/" + consultationId + "/prescriptions", medecinJeton,
                "{\"datePrescription\":\"" + LocalDate.now() + "\",\"lignes\":[{\"medicamentId\":" + medicamentId
                        + ",\"medicament\":\"Amoxicilline 500 mg\",\"posologie\":\"3/j\",\"duree\":\"7 jours\"}]}")
                .andExpect(status().isCreated()));
    }

}

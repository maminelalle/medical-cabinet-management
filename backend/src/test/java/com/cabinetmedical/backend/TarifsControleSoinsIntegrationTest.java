package com.cabinetmedical.backend;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Grille tarifaire de la direction, consultation de controle gratuite et soins (injection, perfusion...). */
class TarifsControleSoinsIntegrationTest extends IntegrationTestBase {

    @Test
    void laDirectionGereLaGrilleTarifaire() throws Exception {
        String direction = connexion("direction@test.local");
        String accueil = connexion("accueil@test.local");

        long cardio = id(envoyer("POST", "/api/actes-catalogue", direction,
                "{\"libelle\":\"Consultation de cardiologie\",\"type\":\"consultation\",\"montantDefaut\":7500,"
                        + "\"specialite\":\"Cardiologie\",\"description\":\"Tarif du cardiologue\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CONSULTATION"))
                .andExpect(jsonPath("$.specialite").value("Cardiologie")));

        envoyer("PUT", "/api/actes-catalogue/" + cardio, direction,
                "{\"libelle\":\"Consultation de cardiologie\",\"type\":\"CONSULTATION\",\"montantDefaut\":8000,"
                        + "\"specialite\":\"Cardiologie\",\"actif\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantDefaut").value(8000))
                .andExpect(jsonPath("$.actif").value(false));
        // Un tarif desactive n'est plus propose a la facturation, mais reste dans la grille de la direction.
        envoyer("GET", "/api/actes-catalogue", accueil, null)
                .andExpect(jsonPath("$[*].id", not(hasItem((int) cardio))));
        envoyer("GET", "/api/actes-catalogue?tous=true", direction, null)
                .andExpect(jsonPath("$[*].id", hasItem((int) cardio)));
        envoyer("PUT", "/api/actes-catalogue/" + cardio, accueil,
                "{\"libelle\":\"X\",\"type\":\"CONSULTATION\",\"montantDefaut\":1}").andExpect(status().isForbidden());

        // Un tarif deja facture ne se supprime pas (historique), un tarif jamais utilise oui.
        long injection = id(envoyer("POST", "/api/actes-catalogue", direction,
                "{\"libelle\":\"Injection\",\"type\":\"SOINS\",\"montantDefaut\":1000}").andExpect(status().isCreated()));
        envoyer("POST", "/api/factures", accueil, "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now()
                + "\",\"lignes\":[{\"catalogueActeId\":" + injection + ",\"libelle\":\"Injection\",\"typeActe\":\"SOINS\",\"montant\":1000}]}")
                .andExpect(status().isCreated());
        envoyer("DELETE", "/api/actes-catalogue/" + injection, direction, null).andExpect(status().isConflict());
        envoyer("DELETE", "/api/actes-catalogue/" + cardio, direction, null).andExpect(status().isNoContent());
    }

    @Test
    void uneConsultationPayeeDonneDroitAUnControleGratuitAvecLeMemeMedecin() throws Exception {
        String accueil = connexion("accueil@test.local");
        String jetonMedecin = connexion("medecin@test.local");
        long consultation = creerRendezVous(accueil, medecin.getId(), creneau(1, 9, 0));
        consulter(jetonMedecin, consultation, "Angine");
        LocalDateTime dansDixJours = creneau(10, 9, 0);

        // Consultation pas encore payee : pas de controle gratuit.
        eligibilite(accueil, medecin.getId(), dansDixJours)
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.message", containsString("pas encore payée")));

        long facture = id(envoyer("POST", "/api/factures", accueil, factureRendezVous(consultation, "5000"))
                .andExpect(status().isCreated()));
        paiement(accueil, facture, "5000").andExpect(status().isOk());

        eligibilite(accueil, medecin.getId(), dansDixJours)
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.rendezVousOrigineId").value(consultation))
                .andExpect(jsonPath("$.restants").value(1));
        eligibilite(accueil, autreMedecin.getId(), dansDixJours).andExpect(jsonPath("$.eligible").value(false));
        eligibilite(accueil, medecin.getId(), creneau(40, 9, 0))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.message", containsString("délai")));

        long controle = id(envoyer("POST", "/api/rendezvous", accueil, controleJson(consultation, dansDixJours))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.controleGratuit").value(true))
                .andExpect(jsonPath("$.rendezVousOrigineId").value(consultation)));
        // Un seul controle gratuit par consultation (regle par defaut).
        envoyer("POST", "/api/rendezvous", accueil, controleJson(consultation, creneau(12, 9, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("déjà atteint")));
        // La facture du controle est a 0 : soldee des sa creation.
        envoyer("POST", "/api/factures", accueil, factureRendezVous(controle, "0"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PAYEE"));

        // La direction regle la gratuite : deux controles autorises.
        envoyer("PUT", "/api/parametres-cabinet/controle-gratuit", accueil, "{\"actif\":true,\"jours\":30,\"nombre\":2}")
                .andExpect(status().isForbidden());
        envoyer("PUT", "/api/parametres-cabinet/controle-gratuit", connexion("direction@test.local"),
                "{\"actif\":true,\"jours\":15,\"nombre\":2}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.jours").value(15));
        envoyer("POST", "/api/rendezvous", accueil, controleJson(consultation, creneau(12, 9, 0)))
                .andExpect(status().isCreated());
    }

    @Test
    void leMedecinProgrammeLeControleEnFinDeConsultation() throws Exception {
        String accueil = connexion("accueil@test.local");
        String jetonMedecin = connexion("medecin@test.local");
        long consultation = creerRendezVous(accueil, medecin.getId(), creneau(1, 10, 0));
        consulter(jetonMedecin, consultation, "Suivi tension");

        String controle = "{\"dateHeure\":\"" + creneau(15, 10, 0) + "\",\"dureeMinutes\":20}";
        envoyer("POST", "/api/rendezvous/" + consultation + "/controle", connexion("cardio@test.local"), controle)
                .andExpect(status().isForbidden());
        long rendezVous = id(envoyer("POST", "/api/rendezvous/" + consultation + "/controle", jetonMedecin, controle)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rendezVousOrigineId").value(consultation))
                .andExpect(jsonPath("$.motif").value("Consultation de contrôle"))
                // Gratuit seulement une fois la consultation payee.
                .andExpect(jsonPath("$.controleGratuit").value(false)));

        long facture = id(envoyer("POST", "/api/factures", accueil, factureRendezVous(consultation, "5000")));
        paiement(accueil, facture, "5000").andExpect(status().isOk());
        envoyer("GET", "/api/rendezvous/" + rendezVous, accueil, null).andExpect(jsonPath("$.controleGratuit").value(true));
    }

    @Test
    void lAccueilEnregistreRealiseEtFactureUnSoin() throws Exception {
        String accueil = connexion("accueil@test.local");
        envoyer("POST", "/api/soins", connexion("direction@test.local"), soinJson()).andExpect(status().isForbidden());

        long soin = id(envoyer("POST", "/api/soins", accueil, soinJson())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.prescripteur").value("Dr. Ely (clinique externe)")));
        envoyer("POST", "/api/soins/" + soin + "/terminer", accueil, "{}").andExpect(status().isConflict());
        envoyer("POST", "/api/soins/" + soin + "/demarrer", accueil, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("EN_COURS"));
        envoyer("POST", "/api/soins/" + soin + "/terminer", accueil, "{\"observations\":\"Bien tolérée\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("TERMINE"))
                .andExpect(jsonPath("$.observations").value("Bien tolérée"));

        String factureSoin = "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now() + "\",\"soinId\":" + soin
                + ",\"lignes\":[{\"libelle\":\"Injection\",\"typeActe\":\"SOINS\",\"montant\":1000}],"
                + "\"paiement\":{\"montant\":1000,\"moyenPaiement\":\"BANKILY\",\"reference\":\"BK-778899\"}}";
        envoyer("POST", "/api/factures", accueil, factureSoin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origine").value("SOIN"))
                .andExpect(jsonPath("$.soinId").value(soin))
                .andExpect(jsonPath("$.statut").value("PAYEE"));
        envoyer("POST", "/api/factures", accueil, factureSoin).andExpect(status().isConflict());
        envoyer("GET", "/api/soins?date=" + LocalDate.now(), connexion("direction@test.local"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].factureStatut").value("PAYEE"));
        envoyer("POST", "/api/soins/" + soin + "/annulation", accueil, "{\"motif\":\"Erreur\"}").andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions eligibilite(String jeton, long medecinId, LocalDateTime date)
            throws Exception {
        return envoyer("GET", "/api/rendezvous/controle-gratuit?patientId=" + patient.getId() + "&medecinId=" + medecinId
                + "&date=" + date, jeton, null).andExpect(status().isOk());
    }

    private String controleJson(long origine, LocalDateTime date) {
        return "{\"patientId\":" + patient.getId() + ",\"medecinId\":" + medecin.getId() + ",\"dateHeure\":\"" + date
                + "\",\"dureeMinutes\":30,\"motif\":\"Contrôle\",\"rendezVousOrigineId\":" + origine + "}";
    }

    private String factureRendezVous(long rendezVousId, String montant) {
        return "{\"patientId\":" + patient.getId() + ",\"dateFacture\":\"" + LocalDate.now() + "\",\"rendezVousId\":" + rendezVousId
                + ",\"lignes\":[{\"libelle\":\"Consultation\",\"typeActe\":\"CONSULTATION\",\"montant\":" + montant + "}]}";
    }

    private String soinJson() {
        return "{\"patientId\":" + patient.getId() + ",\"type\":\"INJECTION\",\"intitule\":\"Injection intramusculaire\","
                + "\"produit\":\"Ceftriaxone 1 g (apporté par le patient)\",\"prescripteurExterne\":\"Dr. Ely (clinique externe)\"}";
    }
}

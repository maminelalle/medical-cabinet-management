-- =============================================================================
-- Jeu de donnees de demonstration du cabinet medical (les trois roles).
-- Mot de passe de tous les comptes de demonstration : password
-- Les dates sont relatives a la date d'execution de la migration afin que la
-- demonstration reste toujours d'actualite (rendez-vous du jour, impayes, etc.)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Catalogue des actes facturables
--    Les types couvrent consultation, hospitalisation, acte chirurgical, etc.
-- ---------------------------------------------------------------------------
INSERT INTO catalogue_actes (libelle, type, montant_defaut) VALUES
    ('Consultation de medecine generale', 'CONSULTATION', 5000.00),
    ('Consultation specialisee', 'CONSULTATION', 7500.00),
    ('Hospitalisation (journee)', 'HOSPITALISATION', 25000.00),
    ('Hospitalisation (nuit supplementaire)', 'HOSPITALISATION', 15000.00),
    ('Petite chirurgie', 'CHIRURGIE', 45000.00),
    ('Chirurgie lourde', 'CHIRURGIE', 80000.00),
    ('Analyse de laboratoire', 'LABORATOIRE', 3500.00),
    ('Echographie', 'IMAGERIE', 8000.00),
    ('Radiographie', 'IMAGERIE', 6000.00),
    ('Dispensation pharmacie', 'PHARMACIE', 2000.00),
    ('Pansement et soins infirmiers', 'SOINS', 1500.00);

-- ---------------------------------------------------------------------------
-- 2. Deux medecins specialistes supplementaires
--    ORDRE-TEST-001 = Dr Marie Dupont (medecine generale, cree par V2)
-- ---------------------------------------------------------------------------
INSERT INTO utilisateurs (email, mot_de_passe, role) VALUES
    ('cardio@test.local', '$2a$10$EP2iuGHyoeL6qnPxhbhYW.XcmP7c.E3OxCMSb2CfI..Yr13wHKc9u', 'MEDECIN'),
    ('chirurgie@test.local', '$2a$10$EP2iuGHyoeL6qnPxhbhYW.XcmP7c.E3OxCMSb2CfI..Yr13wHKc9u', 'MEDECIN');

INSERT INTO medecins (utilisateur_id, nom, prenom, specialite, numero_ordre)
SELECT id, 'Sow', 'Ahmed', 'Cardiologie', 'ORDRE-CARD-002' FROM utilisateurs WHERE email = 'cardio@test.local';

INSERT INTO medecins (utilisateur_id, nom, prenom, specialite, numero_ordre)
SELECT id, 'Camara', 'Fatou', 'Chirurgie', 'ORDRE-CHIR-003' FROM utilisateurs WHERE email = 'chirurgie@test.local';

-- ---------------------------------------------------------------------------
-- 3. Patients
-- ---------------------------------------------------------------------------
INSERT INTO patients (nom, prenom, date_naissance, telephone, email, adresse) VALUES
    ('Diallo', 'Aminata', '1987-04-12', '+222 46 12 34 56', 'aminata.diallo@example.com', 'Tevragh Zeina, Nouakchott'),
    ('Sow', 'Moussa', '1975-11-03', '+222 44 55 66 77', 'moussa.sow@example.com', 'Ksar, Nouakchott'),
    ('Ba', 'Fatima', '1992-06-25', '+222 43 21 65 87', 'fatima.ba@example.com', 'Dar Naim, Nouakchott'),
    ('Kane', 'Ibrahima', '1968-02-17', '+222 45 98 74 12', 'ibrahima.kane@example.com', 'Sebkha, Nouakchott'),
    ('Ndiaye', 'Aissata', '2001-09-08', '+222 47 33 22 11', 'aissata.ndiaye@example.com', 'Arafat, Nouakchott'),
    ('Ould Ahmed', 'Mohamed', '1980-01-30', '+222 46 77 88 99', 'mohamed.ouldahmed@example.com', 'Riyad, Nouakchott'),
    ('Traore', 'Bintou', '1956-12-05', '+222 42 15 96 33', 'bintou.traore@example.com', 'Toujounine, Nouakchott'),
    ('Fall', 'Cheikh', '1990-03-22', '+222 44 11 22 33', 'cheikh.fall@example.com', 'El Mina, Nouakchott');

-- ---------------------------------------------------------------------------
-- 4. Rendez-vous : passes (termines, absent, annule), du jour et a venir
-- ---------------------------------------------------------------------------
INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 12) + TIME '09:00', 'Consultation de suivi', 'TERMINE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 9) + TIME '10:30', 'Bilan cardiologique', 'TERMINE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND m.numero_ordre = 'ORDRE-CARD-002' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 7) + TIME '08:30', 'Controle de tension', 'TERMINE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 5) + TIME '14:00', 'Suivi post-operatoire', 'TERMINE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND m.numero_ordre = 'ORDRE-CHIR-003' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 3) + TIME '11:00', 'Consultation de medecine generale', 'ABSENT', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Ndiaye' AND p.prenom = 'Aissata' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 2) + TIME '15:30', 'Controle cardiologique', 'ANNULE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND m.numero_ordre = 'ORDRE-CARD-002' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE - 1) + TIME '09:30', 'Hospitalisation pour intervention', 'TERMINE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND m.numero_ordre = 'ORDRE-CHIR-003' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, CURRENT_DATE + TIME '08:30', 'Consultation de controle', 'EN_COURS', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, CURRENT_DATE + TIME '09:30', 'Suivi cardiologique', 'CONFIRME', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND m.numero_ordre = 'ORDRE-CARD-002' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, CURRENT_DATE + TIME '10:30', 'Controle de cicatrice', 'CONFIRME', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND m.numero_ordre = 'ORDRE-CHIR-003' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, CURRENT_DATE + TIME '11:30', 'Consultation de medecine generale', 'PLANIFIE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE + 1) + TIME '09:00', 'Controle post-operatoire', 'PLANIFIE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND m.numero_ordre = 'ORDRE-TEST-001' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE + 2) + TIME '10:00', 'Echographie cardiaque', 'PLANIFIE', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND m.numero_ordre = 'ORDRE-CARD-002' AND u.email = 'accueil@test.local';

INSERT INTO rendez_vous (patient_id, medecin_id, date_heure, motif, statut, created_by)
SELECT p.id, m.id, (CURRENT_DATE + 3) + TIME '08:30', 'Petite chirurgie programmee', 'CONFIRME', u.id
FROM patients p, medecins m, utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND m.numero_ordre = 'ORDRE-CHIR-003' AND u.email = 'accueil@test.local';

-- ---------------------------------------------------------------------------
-- 5. Consultations (rendez-vous honores) avec compte-rendu
-- ---------------------------------------------------------------------------
INSERT INTO consultations (rendez_vous_id, compte_rendu)
SELECT r.id, 'Patiente suivie pour asthenie et cephalees. Examen clinique normal, tension 12/8. Bilan biologique demande, repos conseille.'
FROM rendez_vous r JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND r.date_heure = (CURRENT_DATE - 12) + TIME '09:00';

INSERT INTO consultations (rendez_vous_id, compte_rendu)
SELECT r.id, 'Bilan cardiologique : auscultation reguliere, ECG sans anomalie significative. Poursuite du traitement antihypertenseur, controle dans trois mois.'
FROM rendez_vous r JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND r.date_heure = (CURRENT_DATE - 9) + TIME '10:30';

INSERT INTO consultations (rendez_vous_id, compte_rendu)
SELECT r.id, 'Controle de tension arterielle : 13/9 en consultation. Pas de signe fonctionnel. Regles hygieno-dietetiques rappelees.'
FROM rendez_vous r JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND r.date_heure = (CURRENT_DATE - 7) + TIME '08:30';

INSERT INTO consultations (rendez_vous_id, compte_rendu)
SELECT r.id, 'Suivi post-operatoire a J+7 : cicatrice propre, absence de signe infectieux. Ablation des fils realisee, pansement refait.'
FROM rendez_vous r JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND r.date_heure = (CURRENT_DATE - 5) + TIME '14:00';

INSERT INTO consultations (rendez_vous_id, compte_rendu)
SELECT r.id, 'Intervention programmee realisee sans complication. Surveillance post-operatoire satisfaisante, hospitalisation de 24 heures.'
FROM rendez_vous r JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND r.date_heure = (CURRENT_DATE - 1) + TIME '09:30';

-- ---------------------------------------------------------------------------
-- 6. Prescriptions rattachees aux consultations
-- ---------------------------------------------------------------------------
INSERT INTO prescriptions (consultation_id, date_prescription, instructions)
SELECT c.id, CURRENT_DATE - 12, 'Prendre les medicaments apres les repas. Revenir en consultation si la fievre persiste.'
FROM consultations c JOIN rendez_vous r ON r.id = c.rendez_vous_id JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND r.date_heure = (CURRENT_DATE - 12) + TIME '09:00';

INSERT INTO prescriptions (consultation_id, date_prescription, instructions)
SELECT c.id, CURRENT_DATE - 9, 'Traitement a poursuivre au long cours, surveillance de la tension a domicile.'
FROM consultations c JOIN rendez_vous r ON r.id = c.rendez_vous_id JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND r.date_heure = (CURRENT_DATE - 9) + TIME '10:30';

INSERT INTO prescriptions (consultation_id, date_prescription, instructions)
SELECT c.id, CURRENT_DATE - 5, 'Pansement a renouveler tous les deux jours pendant une semaine.'
FROM consultations c JOIN rendez_vous r ON r.id = c.rendez_vous_id JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND r.date_heure = (CURRENT_DATE - 5) + TIME '14:00';

INSERT INTO prescriptions (consultation_id, date_prescription, instructions)
SELECT c.id, CURRENT_DATE - 1, 'Antalgiques en cas de douleur, alimentation legere pendant 48 heures.'
FROM consultations c JOIN rendez_vous r ON r.id = c.rendez_vous_id JOIN patients p ON p.id = r.patient_id
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND r.date_heure = (CURRENT_DATE - 1) + TIME '09:30';

-- ---------------------------------------------------------------------------
-- 7. Lignes de prescription (medicament, posologie, duree)
-- ---------------------------------------------------------------------------
INSERT INTO lignes_prescription (prescription_id, medicament, posologie, duree)
SELECT pr.id, m.medicament, m.posologie, m.duree
FROM prescriptions pr
JOIN consultations c ON c.id = pr.consultation_id
JOIN rendez_vous r ON r.id = c.rendez_vous_id
JOIN patients p ON p.id = r.patient_id
JOIN (VALUES ('Paracetamol 500 mg', '1 comprime matin, midi et soir', '5 jours'),
             ('Fer + acide folique', '1 comprime par jour', '1 mois')) AS m(medicament, posologie, duree) ON TRUE
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND r.date_heure = (CURRENT_DATE - 12) + TIME '09:00';

INSERT INTO lignes_prescription (prescription_id, medicament, posologie, duree)
SELECT pr.id, m.medicament, m.posologie, m.duree
FROM prescriptions pr
JOIN consultations c ON c.id = pr.consultation_id
JOIN rendez_vous r ON r.id = c.rendez_vous_id
JOIN patients p ON p.id = r.patient_id
JOIN (VALUES ('Amlodipine 5 mg', '1 comprime le matin', '3 mois'),
             ('Aspirine 100 mg', '1 comprime le soir', '3 mois')) AS m(medicament, posologie, duree) ON TRUE
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND r.date_heure = (CURRENT_DATE - 9) + TIME '10:30';

INSERT INTO lignes_prescription (prescription_id, medicament, posologie, duree)
SELECT pr.id, m.medicament, m.posologie, m.duree
FROM prescriptions pr
JOIN consultations c ON c.id = pr.consultation_id
JOIN rendez_vous r ON r.id = c.rendez_vous_id
JOIN patients p ON p.id = r.patient_id
JOIN (VALUES ('Ibuprofene 400 mg', '1 comprime matin et soir', '5 jours')) AS m(medicament, posologie, duree) ON TRUE
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND r.date_heure = (CURRENT_DATE - 5) + TIME '14:00';

INSERT INTO lignes_prescription (prescription_id, medicament, posologie, duree)
SELECT pr.id, m.medicament, m.posologie, m.duree
FROM prescriptions pr
JOIN consultations c ON c.id = pr.consultation_id
JOIN rendez_vous r ON r.id = c.rendez_vous_id
JOIN patients p ON p.id = r.patient_id
JOIN (VALUES ('Paracetamol 1 g', '1 comprime toutes les 8 heures', '3 jours'),
             ('Omeprazole 20 mg', '1 gelule le matin a jeun', '7 jours')) AS m(medicament, posologie, duree) ON TRUE
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND r.date_heure = (CURRENT_DATE - 1) + TIME '09:30';

-- ---------------------------------------------------------------------------
-- 8. Factures (multi-actes) couvrant les statuts payee, partielle,
--    en attente et annulee
-- ---------------------------------------------------------------------------
INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 12, 8500.00, 'PAYEE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 9, 21500.00, 'PARTIELLE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 7, 6500.00, 'PAYEE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 5, 79000.00, 'PARTIELLE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 3, 5000.00, 'EN_ATTENTE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Ndiaye' AND p.prenom = 'Aissata' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE, 7000.00, 'PAYEE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE, 7500.00, 'EN_ATTENTE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 2, 8000.00, 'ANNULEE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND u.email = 'accueil@test.local';

INSERT INTO factures (patient_id, date_facture, montant_total, statut, created_by)
SELECT p.id, CURRENT_DATE - 1, 112500.00, 'PARTIELLE', u.id
FROM patients p, utilisateurs u
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND u.email = 'accueil@test.local';

-- ---------------------------------------------------------------------------
-- 9. Lignes de facture : chaque ligne porte un libelle, un type d'acte
--    (CONSULTATION, HOSPITALISATION, CHIRURGIE, ...) et un montant
-- ---------------------------------------------------------------------------
INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation de medecine generale', 'Analyse de laboratoire')
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND f.date_facture = CURRENT_DATE - 12;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation specialisee', 'Echographie', 'Radiographie')
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND f.date_facture = CURRENT_DATE - 9;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation de medecine generale', 'Pansement et soins infirmiers')
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND f.date_facture = CURRENT_DATE - 7;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Petite chirurgie', 'Hospitalisation (journee)', 'Consultation specialisee', 'Pansement et soins infirmiers')
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND f.date_facture = CURRENT_DATE - 5;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation de medecine generale')
WHERE p.nom = 'Ndiaye' AND p.prenom = 'Aissata' AND f.date_facture = CURRENT_DATE - 3;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation de medecine generale', 'Dispensation pharmacie')
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND f.date_facture = CURRENT_DATE;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Consultation specialisee')
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND f.date_facture = CURRENT_DATE;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Echographie')
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND f.date_facture = CURRENT_DATE - 2;

INSERT INTO lignes_facture (facture_id, catalogue_acte_id, libelle, type_acte, montant)
SELECT f.id, a.id, a.libelle, a.type, a.montant_defaut
FROM factures f JOIN patients p ON p.id = f.patient_id
JOIN catalogue_actes a ON a.libelle IN ('Chirurgie lourde', 'Hospitalisation (journee)', 'Consultation specialisee')
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND f.date_facture = CURRENT_DATE - 1;

-- ---------------------------------------------------------------------------
-- 10. Paiements : les statuts des factures correspondent exactement aux
--     sommes encaissees (payee = total, partielle = acompte, en attente = 0)
-- ---------------------------------------------------------------------------
INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 8500.00, (CURRENT_DATE - 12) + TIME '11:30', 'ESPECES', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND f.date_facture = CURRENT_DATE - 12 AND u.email = 'accueil@test.local';

INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 10000.00, (CURRENT_DATE - 9) + TIME '12:15', 'CARTE', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Sow' AND p.prenom = 'Moussa' AND f.date_facture = CURRENT_DATE - 9 AND u.email = 'accueil@test.local';

INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 6500.00, (CURRENT_DATE - 7) + TIME '09:45', 'ESPECES', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Ba' AND p.prenom = 'Fatima' AND f.date_facture = CURRENT_DATE - 7 AND u.email = 'accueil@test.local';

INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 40000.00, (CURRENT_DATE - 5) + TIME '16:20', 'VIREMENT', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Kane' AND p.prenom = 'Ibrahima' AND f.date_facture = CURRENT_DATE - 5 AND u.email = 'accueil@test.local';

INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 7000.00, CURRENT_DATE + TIME '09:05', 'CARTE', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Diallo' AND p.prenom = 'Aminata' AND f.date_facture = CURRENT_DATE AND u.email = 'accueil@test.local';

INSERT INTO paiements (facture_id, montant, date_paiement, moyen_paiement, enregistre_par)
SELECT f.id, 60000.00, (CURRENT_DATE - 1) + TIME '11:00', 'VIREMENT', u.id
FROM factures f JOIN patients p ON p.id = f.patient_id CROSS JOIN utilisateurs u
WHERE p.nom = 'Traore' AND p.prenom = 'Bintou' AND f.date_facture = CURRENT_DATE - 1 AND u.email = 'accueil@test.local';
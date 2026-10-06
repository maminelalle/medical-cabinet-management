-- V13 : donnees de demonstration avec des noms mauritaniens (medecins, personnel et patients).
-- Les migrations V2 et V4 restent inchangees (empreintes Flyway) ; seuls les noms sont remplaces ici.

-- Medecins et comptes associes
UPDATE medecins SET prenom = 'Mohamed', nom = 'Ould Cheikh' WHERE numero_ordre = 'ORDRE-TEST-001';
UPDATE medecins SET prenom = 'Zeinabou', nom = 'Mint Ahmed' WHERE numero_ordre = 'ORDRE-CARD-002';
UPDATE medecins SET prenom = 'Sidi', nom = 'Ould Brahim' WHERE numero_ordre = 'ORDRE-CHIR-003';
UPDATE utilisateurs u SET prenom = m.prenom, nom = m.nom FROM medecins m WHERE m.utilisateur_id = u.id;

-- Personnel
UPDATE utilisateurs SET prenom = 'Khadijetou', nom = 'Mint Salem' WHERE email = 'accueil@test.local';
UPDATE utilisateurs SET prenom = 'Ahmed', nom = 'Ould Abdallahi' WHERE email = 'direction@test.local';
UPDATE utilisateurs SET prenom = 'Moulaye', nom = 'Ould Sidaty' WHERE email = 'pharmacien@test.local';
UPDATE utilisateurs SET prenom = 'El Hacen', nom = 'Ould Mohamed Lemine' WHERE email = 'admin@test.local';

-- Patients
UPDATE patients SET prenom = 'Lalle', nom = 'Ould Mohamed', email = 'lalle.ouldmohamed@example.com'
WHERE nom = 'Diallo' AND prenom = 'Aminata';
UPDATE patients SET prenom = 'Mohamed', nom = 'Ould Sidi', email = 'mohamed.ouldsidi@example.com'
WHERE nom = 'Sow' AND prenom = 'Moussa';
UPDATE patients SET prenom = 'Fatimetou', nom = 'Mint Ahmedou', email = 'fatimetou.mintahmedou@example.com'
WHERE nom = 'Ba' AND prenom = 'Fatima';
UPDATE patients SET prenom = 'Brahim', nom = 'Ould Ely', email = 'brahim.ouldely@example.com'
WHERE nom = 'Kane' AND prenom = 'Ibrahima';
UPDATE patients SET prenom = 'Aichetou', nom = 'Mint Cheikh', email = 'aichetou.mintcheikh@example.com'
WHERE nom = 'Ndiaye' AND prenom = 'Aissata';
UPDATE patients SET prenom = 'Ahmedou', nom = 'Ould Bakar', email = 'ahmedou.ouldbakar@example.com'
WHERE nom = 'Ould Ahmed' AND prenom = 'Mohamed';
UPDATE patients SET prenom = 'Mariem', nom = 'Mint Moctar', email = 'mariem.mintmoctar@example.com'
WHERE nom = 'Traore' AND prenom = 'Bintou';
UPDATE patients SET prenom = 'Cheikh', nom = 'Ould Sidi Mohamed', email = 'cheikh.ouldsidimohamed@example.com'
WHERE nom = 'Fall' AND prenom = 'Cheikh';

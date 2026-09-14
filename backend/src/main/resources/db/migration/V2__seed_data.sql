-- Mot de passe de démonstration pour les trois comptes : password
-- La valeur stockée est un hash BCrypt, jamais le mot de passe en clair.
INSERT INTO utilisateurs (email, mot_de_passe, role)
VALUES
    ('accueil@test.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACCUEIL'),
    ('medecin@test.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEDECIN'),
    ('direction@test.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DIRECTION');

INSERT INTO medecins (utilisateur_id, nom, prenom, specialite, numero_ordre)
SELECT id, 'Dupont', 'Marie', 'Medecine generale', 'ORDRE-TEST-001'
FROM utilisateurs
WHERE email = 'medecin@test.local';
-- Corrige le mot de passe de démonstration sans modifier la migration V2 déjà exécutée.
UPDATE utilisateurs
SET mot_de_passe = '$2a$10$EP2iuGHyoeL6qnPxhbhYW.XcmP7c.E3OxCMSb2CfI..Yr13wHKc9u'
WHERE email IN ('accueil@test.local', 'medecin@test.local', 'direction@test.local');
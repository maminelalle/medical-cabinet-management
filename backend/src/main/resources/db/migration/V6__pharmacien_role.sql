ALTER TABLE utilisateurs DROP CONSTRAINT utilisateurs_role_check;
ALTER TABLE utilisateurs ADD CONSTRAINT utilisateurs_role_check
    CHECK (role IN ('ACCUEIL', 'MEDECIN', 'DIRECTION', 'PHARMACIEN'));

INSERT INTO utilisateurs (email, mot_de_passe, role)
VALUES ('pharmacien@test.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PHARMACIEN')
ON CONFLICT (email) DO NOTHING;
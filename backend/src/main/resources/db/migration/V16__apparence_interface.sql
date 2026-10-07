-- V16 : apparence de l'interface personnalisable par l'administrateur (nom, logo, couleurs).
ALTER TABLE parametres_cabinet ADD COLUMN nom_interface VARCHAR(80) NOT NULL DEFAULT 'Cabinets Médicaux';
ALTER TABLE parametres_cabinet ADD COLUMN couleur_principale VARCHAR(7) NOT NULL DEFAULT '#2563eb'
    CHECK (couleur_principale ~ '^#[0-9a-fA-F]{6}$');
ALTER TABLE parametres_cabinet ADD COLUMN couleur_accent VARCHAR(7) NOT NULL DEFAULT '#10b981'
    CHECK (couleur_accent ~ '^#[0-9a-fA-F]{6}$');
ALTER TABLE parametres_cabinet ADD COLUMN couleur_bouton VARCHAR(7) NOT NULL DEFAULT '#111a2e'
    CHECK (couleur_bouton ~ '^#[0-9a-fA-F]{6}$');
-- Logo en data URL (image PNG, JPEG, WEBP ou SVG de quelques centaines de Ko au plus).
ALTER TABLE parametres_cabinet ADD COLUMN logo TEXT;

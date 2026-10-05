ALTER TABLE medicaments ADD COLUMN prix_achat NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE medicaments ADD COLUMN prix_vente NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE medicaments ADD COLUMN fournisseur VARCHAR(150);
ALTER TABLE medicaments ADD COLUMN date_expiration DATE;

UPDATE medicaments SET prix_vente = prix_unitaire WHERE prix_vente = 0;
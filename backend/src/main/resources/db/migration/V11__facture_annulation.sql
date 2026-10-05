-- Annulation d'une facture : motif obligatoire, date et auteur conserves pour la tracabilite.
ALTER TABLE factures ADD COLUMN motif_annulation VARCHAR(500);
ALTER TABLE factures ADD COLUMN date_annulation TIMESTAMP WITH TIME ZONE;
ALTER TABLE factures ADD COLUMN annulee_par BIGINT REFERENCES utilisateurs(id);

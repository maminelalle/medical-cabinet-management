ALTER TABLE lignes_prescription ADD COLUMN medicament_id BIGINT REFERENCES medicaments(id);

UPDATE lignes_prescription ligne
SET medicament_id = medicament.id
FROM medicaments medicament
WHERE lower(ligne.medicament) LIKE lower(medicament.nom) || '%';
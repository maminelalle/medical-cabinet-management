ALTER TABLE rendez_vous ADD COLUMN numero_file INTEGER;

WITH numerotes AS (
    SELECT id, row_number() OVER (PARTITION BY date(date_heure) ORDER BY date_heure, id) AS numero
    FROM rendez_vous
)
UPDATE rendez_vous rendez
SET numero_file = numerotes.numero
FROM numerotes
WHERE rendez.id = numerotes.id;

ALTER TABLE rendez_vous ALTER COLUMN numero_file SET NOT NULL;
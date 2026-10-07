-- V15 : noms de demonstration simplifies, sans "Ould" ni "Mint" (ex. Lalle Mohamed, Ahmed Sidi).
-- V13 reste inchangee pour preserver les empreintes Flyway.
UPDATE patients SET nom = btrim(regexp_replace(nom, '\m(Ould|Mint)\s+', '', 'gi'))
    WHERE nom ~* '\m(Ould|Mint)\s+';
UPDATE medecins SET nom = btrim(regexp_replace(nom, '\m(Ould|Mint)\s+', '', 'gi'))
    WHERE nom ~* '\m(Ould|Mint)\s+';
UPDATE utilisateurs SET nom = btrim(regexp_replace(nom, '\m(Ould|Mint)\s+', '', 'gi'))
    WHERE nom ~* '\m(Ould|Mint)\s+';

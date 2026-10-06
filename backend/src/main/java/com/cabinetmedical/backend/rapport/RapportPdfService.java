package com.cabinetmedical.backend.rapport;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generation des documents PDF avec JasperReports.
 * Un seul gabarit (rapport-sections.jrxml) : des lignes libelle / detail / valeur regroupees par section.
 */
@Service
public class RapportPdfService {
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private volatile JasperReport gabarit;

    public byte[] generer(EnTeteRapport enTete, List<LigneRapport> lignes) {
        Map<String, Object> parametres = new HashMap<>();
        parametres.put("cabinetNom", enTete.cabinetNom());
        parametres.put("cabinetInfos", enTete.cabinetInfos());
        parametres.put("titre", enTete.titre());
        parametres.put("sousTitre", enTete.sousTitre());
        parametres.put("mention", enTete.mention());
        parametres.put("genereLe", LocalDateTime.now().format(FORMAT_DATE));

        List<Map<String, ?>> donnees = lignes.stream().<Map<String, ?>>map(ligne -> {
            Map<String, Object> valeurs = new HashMap<>();
            valeurs.put("section", ligne.section());
            valeurs.put("libelle", ligne.libelle());
            valeurs.put("detail", ligne.detail());
            valeurs.put("valeur", ligne.valeur());
            return valeurs;
        }).toList();

        try {
            JasperPrint impression = JasperFillManager.fillReport(gabarit(), parametres, new JRMapCollectionDataSource(donnees));
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            JRPdfExporter exporteur = new JRPdfExporter();
            exporteur.setExporterInput(new SimpleExporterInput(impression));
            exporteur.setExporterOutput(new SimpleOutputStreamExporterOutput(sortie));
            exporteur.exportReport();
            return sortie.toByteArray();
        } catch (JRException exception) {
            throw new IllegalStateException("Generation du PDF impossible", exception);
        }
    }

    /** Le gabarit est compile une seule fois, au premier document demande. */
    private JasperReport gabarit() throws JRException {
        if (gabarit == null) {
            synchronized (this) {
                if (gabarit == null) {
                    try (InputStream source = new ClassPathResource("rapports/rapport-sections.jrxml").getInputStream()) {
                        gabarit = JasperCompileManager.compileReport(source);
                    } catch (IOException exception) {
                        throw new JRException("Gabarit de rapport introuvable", exception);
                    }
                }
            }
        }
        return gabarit;
    }

    public record EnTeteRapport(String cabinetNom, String cabinetInfos, String titre, String sousTitre, String mention) {}

    /** Une ligne du rapport ; les lignes d'une meme section doivent etre consecutives. */
    public record LigneRapport(String section, String libelle, String detail, String valeur) {}
}

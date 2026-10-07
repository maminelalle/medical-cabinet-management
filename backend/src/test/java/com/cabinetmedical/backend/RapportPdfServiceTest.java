package com.cabinetmedical.backend;

import com.cabinetmedical.backend.rapport.RapportPdfService;
import com.cabinetmedical.backend.rapport.RapportPdfService.EnTeteRapport;
import com.cabinetmedical.backend.rapport.RapportPdfService.LigneRapport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RapportPdfServiceTest {

    @Test
    void genereUnPdfAvecAccentsEtSections() {
        byte[] pdf = new RapportPdfService().generer(
                new EnTeteRapport("Cabinet Médical", "Nouakchott · Tél. 22 00 00 00", "Dossier patient", "Lalle Mohamed", "Document confidentiel"),
                List.of(new LigneRapport("Identité", "Né(e) le", "12/04/1987", ""),
                        new LigneRapport("Factures", "FAC-001", "Consultation générale", "1 500 MRU")));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}

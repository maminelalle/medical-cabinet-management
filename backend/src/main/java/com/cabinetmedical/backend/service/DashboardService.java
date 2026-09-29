package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ChiffreAffairesResponse;
import com.cabinetmedical.backend.dto.ChiffreAffairesTypeResponse;
import com.cabinetmedical.backend.dto.DashboardConsultationsResponse;
import com.cabinetmedical.backend.dto.FactureResponse;
import com.cabinetmedical.backend.dto.ImpayesResponse;
import com.cabinetmedical.backend.dto.MedecinActiviteResponse;
import com.cabinetmedical.backend.entity.Consultation;
import com.cabinetmedical.backend.entity.Facture;
import com.cabinetmedical.backend.entity.LigneFacture;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.ConsultationRepository;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.LigneFactureRepository;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indicateurs du tableau de bord de la direction, calcules a partir des donnees reelles
 * (rendez-vous, consultations, factures, paiements). Les endpoints acceptent une periode
 * {dateDebut, dateFin} facultative ; par defaut l'activite complete est prise en compte.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ConsultationRepository consultationRepository;
    private final MedecinRepository medecinRepository;
    private final FactureService factureService;

    @Transactional(readOnly = true)
    public DashboardConsultationsResponse consultations(LocalDate dateDebut, LocalDate dateFin) {
        List<RendezVous> rendezVous = rendezVousPeriode(dateDebut, dateFin);
        List<Consultation> consultations = consultationsPeriode(dateDebut, dateFin);
        long termines = rendezVous.stream().filter(item -> item.getStatut() == StatutRendezVous.TERMINE).count();
        return new DashboardConsultationsResponse(dateDebut, dateFin, consultations.size(), rendezVous.size(), termines,
                activiteParMedecin(rendezVous, consultations));
    }

    @Transactional(readOnly = true)
    public List<MedecinActiviteResponse> activiteMedecins(LocalDate dateDebut, LocalDate dateFin) {
        return activiteParMedecin(rendezVousPeriode(dateDebut, dateFin), consultationsPeriode(dateDebut, dateFin));
    }

    @Transactional(readOnly = true)
    public ChiffreAffairesResponse chiffreAffaires(LocalDate dateDebut, LocalDate dateFin) {
        List<Facture> factures = factureRepository.findAllByOrderByDateFactureDesc().stream()
                .filter(facture -> facture.getStatut() != StatutFacture.ANNULEE)
                .filter(facture -> dansPeriode(facture.getDateFacture(), dateDebut, dateFin))
                .toList();

        BigDecimal totalFacture = BigDecimal.ZERO;
        BigDecimal totalEncaisse = BigDecimal.ZERO;
        Map<String, ChiffreAffairesTypeResponse> cumulParType = new LinkedHashMap<>();

        for (Facture facture : factures) {
            FactureResponse response = factureService.versResponse(facture);
            totalFacture = totalFacture.add(response.montantTotal());
            totalEncaisse = totalEncaisse.add(response.montantPaye());
            for (LigneFacture ligne : ligneFactureRepository.findByFactureId(facture.getId())) {
                ChiffreAffairesTypeResponse cumul = cumulParType.get(ligne.getTypeActe());
                cumulParType.put(ligne.getTypeActe(), new ChiffreAffairesTypeResponse(
                        ligne.getTypeActe(),
                        (cumul == null ? BigDecimal.ZERO : cumul.montant()).add(ligne.getMontant()),
                        (cumul == null ? 0 : cumul.nombreActes()) + 1));
            }
        }

        List<ChiffreAffairesTypeResponse> parTypeActe = cumulParType.values().stream()
                .sorted(Comparator.comparing(ChiffreAffairesTypeResponse::montant).reversed())
                .toList();
        return new ChiffreAffairesResponse(dateDebut, dateFin, totalFacture, totalEncaisse,
                totalFacture.subtract(totalEncaisse), parTypeActe);
    }

    @Transactional(readOnly = true)
    public ImpayesResponse impayes() {
        List<FactureResponse> impayees = new ArrayList<>();
        BigDecimal montantFacture = BigDecimal.ZERO;
        BigDecimal montantRestant = BigDecimal.ZERO;
        long nombreFactures = 0;

        for (Facture facture : factureRepository.findAllByOrderByDateFactureDesc()) {
            if (facture.getStatut() == StatutFacture.ANNULEE) {
                continue;
            }
            FactureResponse response = factureService.versResponse(facture);
            nombreFactures++;
            montantFacture = montantFacture.add(response.montantTotal());
            if (response.resteAPayer().compareTo(BigDecimal.ZERO) > 0) {
                impayees.add(response);
                montantRestant = montantRestant.add(response.resteAPayer());
            }
        }

        int taux = nombreFactures == 0 ? 0 : (int) Math.round(impayees.size() * 100.0 / nombreFactures);
        return new ImpayesResponse(nombreFactures, impayees.size(), montantFacture, montantRestant, taux, impayees);
    }

    private List<MedecinActiviteResponse> activiteParMedecin(List<RendezVous> rendezVous, List<Consultation> consultations) {
        return medecinRepository.findAll().stream()
                .map(medecin -> new MedecinActiviteResponse(
                        medecin.getId(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite(),
                        rendezVous.stream().filter(item -> item.getMedecin().getId().equals(medecin.getId())).count(),
                        consultations.stream()
                                .filter(item -> item.getRendezVous().getMedecin().getId().equals(medecin.getId())).count()))
                .sorted(Comparator.comparingLong(MedecinActiviteResponse::consultations).reversed()
                        .thenComparing(MedecinActiviteResponse::nom))
                .toList();
    }

    private List<RendezVous> rendezVousPeriode(LocalDate dateDebut, LocalDate dateFin) {
        return rendezVousRepository.findAllByOrderByDateHeureAsc().stream()
                .filter(item -> dansPeriode(item.getDateHeure().toLocalDate(), dateDebut, dateFin))
                .toList();
    }

    private List<Consultation> consultationsPeriode(LocalDate dateDebut, LocalDate dateFin) {
        return consultationRepository.findAll().stream()
                .filter(item -> dansPeriode(item.getRendezVous().getDateHeure().toLocalDate(), dateDebut, dateFin))
                .toList();
    }

    private boolean dansPeriode(LocalDate date, LocalDate dateDebut, LocalDate dateFin) {
        if (date == null) {
            return false;
        }
        if (dateDebut != null && date.isBefore(dateDebut)) {
            return false;
        }
        return dateFin == null || !date.isAfter(dateFin);
    }
}
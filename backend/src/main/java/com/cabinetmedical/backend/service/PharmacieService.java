package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.repository.DispensationRepository;
import com.cabinetmedical.backend.repository.MedicamentRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PharmacieService {
    private final MedicamentRepository medicamentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DispensationRepository dispensationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FactureService factureService;

    @Transactional(readOnly = true)
    public List<MedicamentResponse> medicaments() {
        return medicamentRepository.findAllByActifTrueOrderByNomAsc().stream().map(MedicamentResponse::from).toList();
    }

    @Transactional
    public MedicamentResponse creerMedicament(MedicamentRequest request) {
        Medicament item = new Medicament();
        appliquer(item, request);
        return MedicamentResponse.from(medicamentRepository.save(item));
    }

    @Transactional
    public List<MedicamentResponse> importerMedicaments(List<MedicamentRequest> requests) {
        return requests.stream().map(this::creerMedicament).toList();
    }

    @Transactional
    public MedicamentResponse modifierStock(Long id, StockRequest request) {
        Medicament item = trouverMedicament(id);
        item.setStockActuel(request.stockActuel());
        return MedicamentResponse.from(item);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionPharmacieResponse> prescriptionsDisponibles() {
        return prescriptionRepository.findAllByOrderByDatePrescriptionDesc().stream()
                .filter(item -> !dispensationRepository.existsByPrescriptionId(item.getId()))
                .map(PrescriptionPharmacieResponse::from).toList();
    }

    @Transactional
    public DispensationResponse dispenser(DispensationRequest request, UserDetails utilisateurConnecte) {
        Prescription prescription = prescriptionRepository.findById(request.prescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription introuvable"));
        if (dispensationRepository.existsByPrescriptionId(prescription.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette prescription a déjà été dispensée");
        }
        Map<Long, Medicament> medicaments = request.lignes().stream()
                .map(line -> trouverMedicament(line.medicamentId()))
                .collect(Collectors.toMap(Medicament::getId, Function.identity(), (first, ignored) -> first));
        for (DispensationLineRequest line : request.lignes()) {
            Medicament item = medicaments.get(line.medicamentId());
            if (item.getStockActuel() < line.quantite()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant pour " + item.getNom());
            }
        }
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
        Dispensation dispensation = new Dispensation();
        dispensation.setPrescription(prescription);
        dispensation.setPatient(prescription.getConsultation().getRendezVous().getPatient());
        dispensation.setDispenseePar(utilisateur);
        request.lignes().forEach(line -> {
            Medicament item = medicaments.get(line.medicamentId());
            item.setStockActuel(item.getStockActuel() - line.quantite());
            LigneDispensation ligne = new LigneDispensation();
            ligne.setDispensation(dispensation);
            ligne.setMedicament(item);
            ligne.setQuantite(line.quantite());
            dispensation.getLignes().add(ligne);
        });
        Dispensation saved = dispensationRepository.save(dispensation);
        List<LigneFactureRequest> lignesFacture = new ArrayList<>();
        for (DispensationLineRequest line : request.lignes()) {
            Medicament item = medicaments.get(line.medicamentId());
            BigDecimal montant = item.getPrixVente().multiply(BigDecimal.valueOf(line.quantite()));
            lignesFacture.add(new LigneFactureRequest(null,
                item.getNom() + (item.getDosage() == null ? "" : " " + item.getDosage()) + " x" + line.quantite(),
                "PHARMACIE", montant));
        }
        FactureResponse facture = factureService.creer(
            new FactureRequest(saved.getPatient().getId(), LocalDate.now(), lignesFacture), utilisateurConnecte);
        return new DispensationResponse(saved.getId(), saved.getPrescription().getId(), saved.getPatient().getId(),
            facture.id(), saved.getDateDispensation(), saved.getLignes().stream()
            .map(line -> MedicamentResponse.from(line.getMedicament())).toList());
    }

    private Medicament trouverMedicament(Long id) {
        return medicamentRepository.findById(id)
                .filter(Medicament::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médicament introuvable"));
    }

    private void appliquer(Medicament item, MedicamentRequest request) {
        item.setNom(request.nom().trim());
        item.setDosage(request.dosage());
        item.setForme(request.forme());
        item.setStockActuel(request.stockActuel());
        item.setSeuilAlerte(request.seuilAlerte());
        item.setPrixAchat(request.prixAchat());
        item.setPrixVente(request.prixVente());
        item.setPrixUnitaire(request.prixVente());
        item.setFournisseur(request.fournisseur());
        item.setDateExpiration(request.dateExpiration());
    }
}
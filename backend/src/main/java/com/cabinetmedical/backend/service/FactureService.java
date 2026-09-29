package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureService {
    private final FactureRepository factureRepository;
    private final PatientRepository patientRepository;
    private final CatalogueActeRepository catalogueActeRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final PaiementRepository paiementRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<FactureResponse> lister(StatutFacture statut) {
        return factureRepository.findAll().stream()
                .filter(facture -> statut == null || facture.getStatut() == statut)
                .map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public FactureResponse trouver(Long id) { return versResponse(trouverFacture(id)); }

    @Transactional
    public FactureResponse creer(FactureRequest request, UserDetails utilisateurConnecte) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
        Facture facture = new Facture();
        facture.setPatient(patient);
        facture.setDateFacture(request.dateFacture());
        facture.setCreatedBy(utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername()).orElse(null));
        BigDecimal total = BigDecimal.ZERO;
        for (LigneFactureRequest requestLigne : request.lignes()) {
            CatalogueActe acte = requestLigne.catalogueActeId() == null ? null : catalogueActeRepository.findById(requestLigne.catalogueActeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte du catalogue introuvable"));
            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(facture);
            ligne.setCatalogueActe(acte);
            ligne.setLibelle(requestLigne.libelle());
            ligne.setTypeActe(requestLigne.typeActe().toUpperCase());
            ligne.setMontant(requestLigne.montant());
            facture.getLignes().add(ligne);
            total = total.add(requestLigne.montant());
        }
        facture.setMontantTotal(total);
        return versResponse(factureRepository.save(facture));
    }

    @Transactional
    public FactureResponse ajouterPaiement(Long factureId, PaiementRequest request, UserDetails utilisateurConnecte) {
        Facture facture = trouverFacture(factureId);
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture annulée ne peut pas être encaissée");
        }
        BigDecimal dejaPaye = paiementRepository.sumMontantByFactureId(factureId);
        if (request.montant().compareTo(facture.getMontantTotal().subtract(dejaPaye)) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Le paiement dépasse le reste à payer");
        }
        Paiement paiement = new Paiement();
        paiement.setFacture(facture);
        paiement.setMontant(request.montant());
        paiement.setMoyenPaiement(request.moyenPaiement());
        paiement.setEnregistrePar(utilisateurRepository.findByEmailIgnoreCase(utilisateurConnecte.getUsername()).orElse(null));
        paiementRepository.save(paiement);
        recalculerStatut(facture, dejaPaye.add(request.montant()));
        factureRepository.save(facture);
        return versResponse(facture);
    }

    private void recalculerStatut(Facture facture, BigDecimal montantPaye) {
        if (montantPaye.compareTo(BigDecimal.ZERO) == 0) facture.setStatut(StatutFacture.EN_ATTENTE);
        else if (montantPaye.compareTo(facture.getMontantTotal()) >= 0) facture.setStatut(StatutFacture.PAYEE);
        else facture.setStatut(StatutFacture.PARTIELLE);
    }

    public FactureResponse versResponse(Facture facture) {
        BigDecimal montantPaye = paiementRepository.sumMontantByFactureId(facture.getId());
        List<LigneFactureResponse> lignes = ligneFactureRepository.findByFactureId(facture.getId()).stream().map(LigneFactureResponse::from).toList();
        List<PaiementResponse> paiements = paiementRepository.findByFactureIdOrderByDatePaiementDesc(facture.getId()).stream().map(PaiementResponse::from).toList();
        return FactureResponse.from(facture, montantPaye, lignes, paiements);
    }

    private Facture trouverFacture(Long id) {
        return factureRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
    }
}
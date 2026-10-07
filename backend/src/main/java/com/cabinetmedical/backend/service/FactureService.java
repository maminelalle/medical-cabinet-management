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
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureService {
    private final FactureRepository factureRepository;
    private final PatientRepository patientRepository;
    private final CatalogueActeRepository catalogueActeRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final PaiementRepository paiementRepository;
    private final RendezVousRepository rendezVousRepository;
    private final ActeProgrammeRepository acteProgrammeRepository;
    private final DispensationRepository dispensationRepository;
    private final SoinRepository soinRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    @Transactional(readOnly = true)
    public List<FactureResponse> lister(StatutFacture statut) {
        return factureRepository.findAll().stream()
                .filter(facture -> statut == null || facture.getStatut() == statut)
                .sorted(Comparator.comparing(Facture::getDateFacture).thenComparing(Facture::getId).reversed())
                .map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public FactureResponse trouver(Long id) { return versResponse(trouverFacture(id)); }

    /**
     * Cree la facture. Rattachee a un rendez-vous ou a un acte programme, elle doit concerner le meme patient
     * et etre la seule facture active de cette origine. Avec {@code paiement}, elle est encaissee aussitot.
     */
    @Transactional
    public FactureResponse creer(FactureRequest request, UserDetails connecte) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
        Facture facture = new Facture();
        facture.setPatient(patient);
        facture.setDateFacture(request.dateFacture());
        facture.setCreatedBy(utilisateurConnecte.utilisateur(connecte));
        rattacherOrigine(facture, request);

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
        // Controle gratuit ou acte offert : rien a encaisser, la facture est soldee des sa creation.
        if (total.signum() == 0) facture.setStatut(StatutFacture.PAYEE);
        Facture enregistree = factureRepository.save(facture);
        if (total.signum() == 0) return versResponse(enregistree);
        if (request.paiement() != null) {
            return ajouterPaiement(enregistree.getId(), request.paiement(), connecte);
        }
        return versResponse(enregistree);
    }

    @Transactional
    public FactureResponse ajouterPaiement(Long factureId, PaiementRequest request, UserDetails connecte) {
        Facture facture = trouverFacture(factureId);
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture annulée ne peut pas être encaissée");
        }
        String[] moyenEtReference = MoyensPaiement.valider(request.moyenPaiement(), request.reference());
        BigDecimal dejaPaye = paiementRepository.sumMontantByFactureId(factureId);
        if (request.montant().compareTo(facture.getMontantTotal().subtract(dejaPaye)) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Le paiement dépasse le reste à payer");
        }
        Paiement paiement = new Paiement();
        paiement.setFacture(facture);
        paiement.setMontant(request.montant());
        paiement.setMoyenPaiement(moyenEtReference[0]);
        paiement.setReference(moyenEtReference[1]);
        paiement.setEnregistrePar(utilisateurConnecte.utilisateur(connecte));
        paiementRepository.save(paiement);
        recalculerStatut(facture, dejaPaye.add(request.montant()));
        factureRepository.save(facture);
        return versResponse(facture);
    }

    /**
     * Annule une facture saisie par erreur. Refusee si un paiement a deja ete encaisse :
     * un encaissement ne disparait pas, il doit d'abord etre rembourse hors application.
     */
    @Transactional
    public FactureResponse annuler(Long factureId, AnnulationFactureRequest request, UserDetails connecte) {
        Facture facture = trouverFacture(factureId);
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette facture est déjà annulée");
        }
        if (paiementRepository.sumMontantByFactureId(factureId).compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible d'annuler une facture qui a déjà reçu un paiement");
        }
        facture.setStatut(StatutFacture.ANNULEE);
        facture.setMotifAnnulation(request.motif().trim());
        facture.setDateAnnulation(Instant.now());
        facture.setAnnuleePar(utilisateurConnecte.utilisateur(connecte));
        return versResponse(factureRepository.save(facture));
    }

    private void rattacherOrigine(Facture facture, FactureRequest request) {
        long origines = java.util.stream.Stream.of(request.rendezVousId(), request.acteProgrammeId(), request.soinId())
                .filter(java.util.Objects::nonNull).count();
        if (origines > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une facture concerne un seul rendez-vous, acte ou soin");
        }
        if (request.soinId() != null) {
            Soin soin = soinRepository.findById(request.soinId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soin introuvable"));
            verifierMemePatient(soin.getPatient(), facture.getPatient());
            if (soin.getStatut() == StatutSoin.ANNULE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce soin est annulé : il ne peut pas être facturé");
            }
            verifierAucuneFactureActive(factureRepository.findBySoinId(soin.getId()), "ce soin");
            facture.setSoin(soin);
        }
        if (request.rendezVousId() != null) {
            RendezVous rendezVous = rendezVousRepository.findById(request.rendezVousId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rendez-vous introuvable"));
            verifierMemePatient(rendezVous.getPatient(), facture.getPatient());
            if (rendezVous.getStatut() == StatutRendezVous.ANNULE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce rendez-vous est annulé : il ne peut pas être facturé");
            }
            verifierAucuneFactureActive(factureRepository.findByRendezVousId(rendezVous.getId()), "ce rendez-vous");
            facture.setRendezVous(rendezVous);
        }
        if (request.acteProgrammeId() != null) {
            ActeProgramme acte = acteProgrammeRepository.findById(request.acteProgrammeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte programmé introuvable"));
            verifierMemePatient(acte.getPatient(), facture.getPatient());
            if (acte.getStatut() == StatutActe.ANNULE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet acte est annulé : il ne peut pas être facturé");
            }
            verifierAucuneFactureActive(factureRepository.findByActeProgrammeId(acte.getId()), "cet acte");
            facture.setActeProgramme(acte);
        }
    }

    private static void verifierMemePatient(Patient origine, Patient facture) {
        if (!origine.getId().equals(facture.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La facture doit concerner le même patient que son origine");
        }
    }

    private static void verifierAucuneFactureActive(List<Facture> factures, String origine) {
        factures.stream().filter(facture -> facture.getStatut() != StatutFacture.ANNULEE).findFirst().ifPresent(facture -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "La facture FAC-%03d existe déjà pour %s", facture.getId(), origine));
        });
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
        Long dispensationId = dispensationRepository.findByFactureId(facture.getId()).map(Dispensation::getId).orElse(null);
        return FactureResponse.from(facture, montantPaye, lignes, paiements, dispensationId);
    }

    private Facture trouverFacture(Long id) {
        return factureRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
    }
}

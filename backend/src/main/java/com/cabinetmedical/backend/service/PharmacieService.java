package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.*;
import com.cabinetmedical.backend.dto.PharmacieFinancesResponse.MontantParLibelle;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.rapport.RapportPdfService;
import com.cabinetmedical.backend.rapport.RapportPdfService.EnTeteRapport;
import com.cabinetmedical.backend.rapport.RapportPdfService.LigneRapport;
import com.cabinetmedical.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pharmacie interne : catalogue et stock, mouvements (achats, ventes, ajustements), vente des ordonnances
 * avec paiement obligatoire, et suivi financier.
 */
@Service
@RequiredArgsConstructor
public class PharmacieService {
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final MedicamentRepository medicamentRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DispensationRepository dispensationRepository;
    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final FactureService factureService;
    private final UtilisateurConnecte utilisateurConnecte;
    private final RapportPdfService rapportPdfService;
    private final ParametresCabinetService parametresCabinetService;

    @Transactional(readOnly = true)
    public List<MedicamentResponse> medicaments() {
        return medicamentRepository.findAllByActifTrueOrderByNomAsc().stream().map(MedicamentResponse::from).toList();
    }

    @Transactional
    public MedicamentResponse creerMedicament(MedicamentRequest request, UserDetails connecte) {
        Medicament item = new Medicament();
        appliquer(item, request);
        item.setStockActuel(request.stockActuel());
        Medicament enregistre = medicamentRepository.save(item);
        if (request.stockActuel() > 0) {
            mouvement(enregistre, TypeMouvement.ENTREE_INITIALE, request.stockActuel(), request.prixAchat(),
                    request.fournisseur(), null, "Stock initial", null, connecte);
        }
        return MedicamentResponse.from(enregistre);
    }

    @Transactional
    public List<MedicamentResponse> importerMedicaments(List<MedicamentRequest> requests, UserDetails connecte) {
        return requests.stream().map(request -> creerMedicament(request, connecte)).toList();
    }

    @Transactional
    public MedicamentResponse modifierMedicament(Long id, MedicamentRequest request) {
        Medicament item = trouverMedicament(id);
        appliquer(item, request);
        return MedicamentResponse.from(item);
    }

    /** Correction d'inventaire : l'ecart est trace en mouvement d'ajustement. */
    @Transactional
    public MedicamentResponse modifierStock(Long id, StockRequest request, UserDetails connecte) {
        Medicament item = trouverMedicament(id);
        int ecart = request.stockActuel() - item.getStockActuel();
        if (ecart != 0) {
            mouvement(item, TypeMouvement.AJUSTEMENT, ecart, item.getPrixAchat(), null, null,
                    request.commentaire() == null || request.commentaire().isBlank() ? "Correction d'inventaire" : request.commentaire(),
                    null, connecte);
        }
        return MedicamentResponse.from(item);
    }

    /** Achat fournisseur : entree en stock, mise a jour du prix d'achat et du fournisseur. */
    @Transactional
    public MedicamentResponse approvisionner(Long id, ApprovisionnementRequest request, UserDetails connecte) {
        Medicament item = trouverMedicament(id);
        item.setPrixAchat(request.prixAchatUnitaire());
        if (request.fournisseur() != null && !request.fournisseur().isBlank()) item.setFournisseur(request.fournisseur().trim());
        if (request.dateExpiration() != null) item.setDateExpiration(request.dateExpiration());
        mouvement(item, TypeMouvement.ACHAT, request.quantite(), request.prixAchatUnitaire(), item.getFournisseur(),
                request.reference(), "Approvisionnement", null, connecte);
        return MedicamentResponse.from(item);
    }

    @Transactional(readOnly = true)
    public MedicamentDetailResponse details(Long id) {
        Medicament item = medicamentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médicament introuvable"));
        List<MouvementStock> mouvements = mouvementStockRepository.findByMedicamentIdOrderByDateMouvementDescIdDesc(id);
        int vendu = mouvements.stream().filter(m -> m.getType() == TypeMouvement.VENTE).mapToInt(m -> -m.getQuantite()).sum();
        BigDecimal ventes = somme(mouvements, TypeMouvement.VENTE);
        int achete = mouvements.stream().filter(m -> m.getType() == TypeMouvement.ACHAT).mapToInt(MouvementStock::getQuantite).sum();
        BigDecimal achats = somme(mouvements, TypeMouvement.ACHAT);
        BigDecimal marge = ventes.subtract(item.getPrixAchat().multiply(BigDecimal.valueOf(vendu)));
        return new MedicamentDetailResponse(MedicamentResponse.from(item), vendu, ventes, achete, achats, marge,
                item.getPrixAchat().multiply(BigDecimal.valueOf(item.getStockActuel())),
                item.getPrixVente().multiply(BigDecimal.valueOf(item.getStockActuel())),
                derniere(mouvements, TypeMouvement.VENTE), derniere(mouvements, TypeMouvement.ACHAT),
                mouvements.stream().map(MouvementStockResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionPharmacieResponse> prescriptionsDisponibles() {
        return prescriptionRepository.findAllByOrderByDatePrescriptionDesc().stream()
                .filter(item -> !dispensationRepository.existsByPrescriptionId(item.getId()))
                .map(PrescriptionPharmacieResponse::from).toList();
    }

    /**
     * Vente d'une ordonnance : decremente le stock, trace les sorties, cree la facture pharmacie
     * et enregistre le paiement (moyen et reference) dans la meme operation.
     */
    @Transactional
    public DispensationResponse dispenser(DispensationRequest request, UserDetails connecte) {
        String[] moyenEtReference = MoyensPaiement.valider(request.moyenPaiement(), request.referencePaiement());
        Prescription prescription = prescriptionRepository.findById(request.prescriptionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription introuvable"));
        if (dispensationRepository.existsByPrescriptionId(prescription.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette prescription a déjà été dispensée");
        }
        Map<Long, Medicament> medicaments = request.lignes().stream()
                .map(line -> trouverMedicament(line.medicamentId()))
                .collect(Collectors.toMap(Medicament::getId, Function.identity(), (premier, ignore) -> premier));
        Map<Long, Integer> quantites = new HashMap<>();
        request.lignes().forEach(line -> quantites.merge(line.medicamentId(), line.quantite(), Integer::sum));
        quantites.forEach((medicamentId, quantite) -> {
            Medicament item = medicaments.get(medicamentId);
            if (item.getStockActuel() < quantite) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuffisant pour " + item.getNom() + " (" + item.getStockActuel() + " disponible(s))");
            }
        });

        Dispensation dispensation = new Dispensation();
        dispensation.setPrescription(prescription);
        dispensation.setPatient(prescription.getConsultation().getRendezVous().getPatient());
        dispensation.setDispenseePar(utilisateurConnecte.utilisateur(connecte));
        request.lignes().forEach(line -> {
            LigneDispensation ligne = new LigneDispensation();
            ligne.setDispensation(dispensation);
            ligne.setMedicament(medicaments.get(line.medicamentId()));
            ligne.setQuantite(line.quantite());
            dispensation.getLignes().add(ligne);
        });
        Dispensation enregistree = dispensationRepository.save(dispensation);

        List<LigneFactureRequest> lignesFacture = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DispensationLineRequest line : request.lignes()) {
            Medicament item = medicaments.get(line.medicamentId());
            mouvement(item, TypeMouvement.VENTE, -line.quantite(), item.getPrixVente(), null, null, null, enregistree, connecte);
            BigDecimal montant = item.getPrixVente().multiply(BigDecimal.valueOf(line.quantite()));
            total = total.add(montant);
            lignesFacture.add(new LigneFactureRequest(null,
                    item.getNom() + (item.getDosage() == null ? "" : " " + item.getDosage()) + " x" + line.quantite(),
                    "PHARMACIE", montant));
        }
        PaiementRequest paiement = total.signum() > 0 ? new PaiementRequest(total, moyenEtReference[0], moyenEtReference[1]) : null;
        FactureResponse facture = factureService.creer(new FactureRequest(enregistree.getPatient().getId(), LocalDate.now(),
                lignesFacture, null, null, paiement), connecte);
        enregistree.setFacture(factureRepository.findById(facture.id()).orElseThrow());
        return new DispensationResponse(enregistree.getId(), prescription.getId(), enregistree.getPatient().getId(),
                facture.id(), enregistree.getDateDispensation(),
                enregistree.getLignes().stream().map(line -> MedicamentResponse.from(line.getMedicament())).toList());
    }

    @Transactional(readOnly = true)
    public PharmacieFinancesResponse finances(LocalDate dateDebut, LocalDate dateFin) {
        LocalDate fin = dateFin != null ? dateFin : LocalDate.now();
        LocalDate debut = dateDebut != null ? dateDebut : fin.withDayOfMonth(1);
        Instant debutInstant = debut.atStartOfDay(ZONE).toInstant();
        Instant finInstant = fin.plusDays(1).atStartOfDay(ZONE).toInstant();
        List<MouvementStock> mouvements = mouvementStockRepository
                .findByDateMouvementGreaterThanEqualAndDateMouvementLessThanOrderByDateMouvementDescIdDesc(debutInstant, finInstant);

        BigDecimal ventes = somme(mouvements, TypeMouvement.VENTE);
        BigDecimal achats = somme(mouvements, TypeMouvement.ACHAT);
        BigDecimal coutDesVentes = mouvements.stream().filter(m -> m.getType() == TypeMouvement.VENTE)
                .map(m -> m.getMedicament().getPrixAchat().multiply(BigDecimal.valueOf(-m.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Dispensation> dispensations = dispensationRepository
                .findByDateDispensationGreaterThanEqualAndDateDispensationLessThan(debutInstant, finInstant);
        Map<String, MontantParLibelle> parMoyen = new TreeMap<>();
        BigDecimal encaisse = BigDecimal.ZERO;
        BigDecimal reste = BigDecimal.ZERO;
        for (Dispensation dispensation : dispensations) {
            Facture facture = dispensation.getFacture();
            if (facture == null || facture.getStatut() == StatutFacture.ANNULEE) continue;
            BigDecimal paye = BigDecimal.ZERO;
            for (Paiement paiement : paiementRepository.findByFactureIdOrderByDatePaiementDesc(facture.getId())) {
                paye = paye.add(paiement.getMontant());
                String moyen = paiement.getMoyenPaiement() == null ? "AUTRE" : paiement.getMoyenPaiement();
                MontantParLibelle actuel = parMoyen.getOrDefault(moyen, new MontantParLibelle(moyen, 0, BigDecimal.ZERO));
                parMoyen.put(moyen, new MontantParLibelle(moyen, actuel.nombre() + 1, actuel.montant().add(paiement.getMontant())));
            }
            encaisse = encaisse.add(paye);
            reste = reste.add(facture.getMontantTotal().subtract(paye));
        }

        Map<String, MontantParLibelle> parMedicament = new HashMap<>();
        mouvements.stream().filter(m -> m.getType() == TypeMouvement.VENTE).forEach(m -> {
            String nom = m.getMedicament().getNom();
            MontantParLibelle actuel = parMedicament.getOrDefault(nom, new MontantParLibelle(nom, 0, BigDecimal.ZERO));
            parMedicament.put(nom, new MontantParLibelle(nom, actuel.nombre() - m.getQuantite(),
                    actuel.montant().add(m.getMontant() == null ? BigDecimal.ZERO : m.getMontant())));
        });

        List<Medicament> stock = medicamentRepository.findAllByActifTrueOrderByNomAsc();
        return new PharmacieFinancesResponse(debut, fin, ventes, achats, ventes.subtract(coutDesVentes), encaisse, reste,
                dispensations.size(),
                stock.stream().map(m -> m.getPrixAchat().multiply(BigDecimal.valueOf(m.getStockActuel()))).reduce(BigDecimal.ZERO, BigDecimal::add),
                stock.stream().map(m -> m.getPrixVente().multiply(BigDecimal.valueOf(m.getStockActuel()))).reduce(BigDecimal.ZERO, BigDecimal::add),
                new ArrayList<>(parMoyen.values()),
                parMedicament.values().stream().sorted(Comparator.comparing(MontantParLibelle::montant).reversed()).limit(5).toList(),
                mouvements.stream().limit(300).map(MouvementStockResponse::from).toList());
    }

    /** Inventaire du stock en PDF (JasperReports), regroupe par famille. */
    @Transactional(readOnly = true)
    public byte[] inventairePdf() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        List<LigneRapport> lignes = medicamentRepository.findAllByActifTrueOrderByNomAsc().stream()
                .sorted(Comparator.comparing((Medicament m) -> m.getFamille() == null ? "~" : m.getFamille()).thenComparing(Medicament::getNom))
                .map(m -> new LigneRapport(m.getFamille() == null ? "Sans famille" : m.getFamille(),
                        m.getNom() + (m.getDosage() == null ? "" : " " + m.getDosage()),
                        (m.getForme() == null ? "" : m.getForme() + " · ") + "Stock " + m.getStockActuel() + " (seuil " + m.getSeuilAlerte() + ")"
                                + (m.getFournisseur() == null ? "" : " · " + m.getFournisseur())
                                + (m.getDateExpiration() == null ? "" : " · exp. " + m.getDateExpiration()),
                        format.format(m.getPrixVente()) + " MRU"))
                .toList();
        var cabinet = parametresCabinetService.lire();
        return rapportPdfService.generer(new EnTeteRapport(cabinet.nom(), cabinet.coordonnees(), "Inventaire pharmacie",
                lignes.size() + " référence(s) en stock", "Prix de vente unitaire indiqué à droite"), lignes);
    }

    private void mouvement(Medicament item, TypeMouvement type, int quantite, BigDecimal prixUnitaire, String fournisseur,
                           String reference, String commentaire, Dispensation dispensation, UserDetails connecte) {
        item.setStockActuel(item.getStockActuel() + (type == TypeMouvement.ENTREE_INITIALE ? 0 : quantite));
        MouvementStock mouvement = new MouvementStock();
        mouvement.setMedicament(item);
        mouvement.setType(type);
        mouvement.setQuantite(quantite);
        mouvement.setStockApres(item.getStockActuel());
        mouvement.setPrixUnitaire(prixUnitaire);
        mouvement.setMontant(prixUnitaire == null ? null : prixUnitaire.multiply(BigDecimal.valueOf(Math.abs(quantite))));
        mouvement.setFournisseur(fournisseur);
        mouvement.setReference(reference);
        mouvement.setCommentaire(commentaire);
        mouvement.setDispensation(dispensation);
        mouvement.setUtilisateur(utilisateurConnecte.utilisateur(connecte));
        mouvementStockRepository.save(mouvement);
    }

    private static BigDecimal somme(List<MouvementStock> mouvements, TypeMouvement type) {
        return mouvements.stream().filter(m -> m.getType() == type && m.getMontant() != null)
                .map(MouvementStock::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Instant derniere(List<MouvementStock> mouvements, TypeMouvement type) {
        return mouvements.stream().filter(m -> m.getType() == type).map(MouvementStock::getDateMouvement)
                .max(Comparator.naturalOrder()).orElse(null);
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
        item.setFamille(request.famille() == null || request.famille().isBlank() ? null : request.famille().trim());
        item.setSeuilAlerte(request.seuilAlerte());
        item.setPrixAchat(request.prixAchat());
        item.setPrixVente(request.prixVente());
        item.setPrixUnitaire(request.prixVente());
        item.setFournisseur(request.fournisseur());
        item.setDateExpiration(request.dateExpiration());
    }
}

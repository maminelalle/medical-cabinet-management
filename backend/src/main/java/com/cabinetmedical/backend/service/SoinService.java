package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.FinSoinRequest;
import com.cabinetmedical.backend.dto.MotifRequest;
import com.cabinetmedical.backend.dto.SoinRequest;
import com.cabinetmedical.backend.dto.SoinResponse;
import com.cabinetmedical.backend.entity.*;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.PrescriptionRepository;
import com.cabinetmedical.backend.repository.SoinRepository;
import com.cabinetmedical.backend.temps.TempsReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Soins au cabinet (injection, perfusion, pansement, nebulisation, constantes) : enregistres par l'accueil,
 * realises par l'infirmier ou le medecin (en attente -> en cours -> termine), puis factures par l'accueil.
 */
@Service
@RequiredArgsConstructor
public class SoinService {
    private final SoinRepository soinRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final FactureRepository factureRepository;
    private final PatientService patientService;
    private final UtilisateurConnecte utilisateurConnecte;
    private final TempsReelService tempsReelService;

    @Transactional(readOnly = true)
    public List<SoinResponse> lister(LocalDate date, Long patientId, StatutSoin statut) {
        List<Soin> soins = patientId != null ? soinRepository.findByPatientIdOrderByDateHeureDesc(patientId)
                : soinRepository.findAllByOrderByDateHeureDesc();
        return soins.stream()
                .filter(soin -> date == null || soin.getDateHeure().toLocalDate().equals(date))
                .filter(soin -> statut == null || soin.getStatut() == statut)
                .map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public SoinResponse trouver(Long id) { return versResponse(trouverSoin(id)); }

    @Transactional
    public SoinResponse creer(SoinRequest request, UserDetails connecte) {
        Soin soin = new Soin();
        soin.setPatient(patientDeLaDemande(request));
        appliquer(soin, request);
        soin.setCreatedBy(utilisateurConnecte.utilisateur(connecte));
        return enregistrer(soin);
    }

    @Transactional
    public SoinResponse modifier(Long id, SoinRequest request) {
        Soin soin = trouverSoin(id);
        if (soin.getStatut() != StatutSoin.EN_ATTENTE) refuser("Seul un soin en attente peut être modifié");
        if (request.patientId() != null) soin.setPatient(trouverPatient(request.patientId()));
        appliquer(soin, request);
        return enregistrer(soin);
    }

    @Transactional
    public SoinResponse demarrer(Long id, UserDetails connecte) {
        Soin soin = trouverSoin(id);
        if (soin.getStatut() != StatutSoin.EN_ATTENTE) refuser("Seul un soin en attente peut démarrer");
        soin.setStatut(StatutSoin.EN_COURS);
        soin.setDebut(Instant.now());
        soin.setRealisePar(utilisateurConnecte.utilisateur(connecte));
        return enregistrer(soin);
    }

    @Transactional
    public SoinResponse terminer(Long id, FinSoinRequest request, UserDetails connecte) {
        Soin soin = trouverSoin(id);
        if (soin.getStatut() != StatutSoin.EN_COURS) refuser("Démarrez le soin avant de le terminer");
        soin.setStatut(StatutSoin.TERMINE);
        soin.setFin(Instant.now());
        if (request != null && request.observations() != null && !request.observations().isBlank()) {
            soin.setObservations(request.observations().trim());
        }
        if (soin.getRealisePar() == null) soin.setRealisePar(utilisateurConnecte.utilisateur(connecte));
        return enregistrer(soin);
    }

    @Transactional
    public SoinResponse annuler(Long id, MotifRequest request) {
        Soin soin = trouverSoin(id);
        if (soin.getStatut() == StatutSoin.TERMINE || soin.getStatut() == StatutSoin.ANNULE) {
            refuser("Ce soin est clôturé : il ne peut plus être annulé");
        }
        Facture facture = factureActive(soin.getId());
        if (facture != null) {
            refuser("Ce soin a la facture FAC-" + String.format("%03d", facture.getId()) + " : annulez d'abord la facture");
        }
        soin.setStatut(StatutSoin.ANNULE);
        String motif = request == null || request.motif() == null ? "" : request.motif().trim();
        if (!motif.isEmpty()) {
            soin.setObservations((soin.getObservations() == null ? "" : soin.getObservations() + "\n") + "Annulé : " + motif);
        }
        return enregistrer(soin);
    }

    public SoinResponse versResponse(Soin soin) {
        return SoinResponse.from(soin, factureActive(soin.getId()));
    }

    private SoinResponse enregistrer(Soin soin) {
        Soin enregistre = soinRepository.save(soin);
        tempsReelService.diffuserApresValidation("SOINS", null, null);
        return versResponse(enregistre);
    }

    private void appliquer(Soin soin, SoinRequest request) {
        soin.setType(request.type());
        soin.setIntitule(request.intitule().trim());
        soin.setProduit(vide(request.produit()));
        soin.setDateHeure(request.dateHeure() == null ? LocalDateTime.now().withSecond(0).withNano(0) : request.dateHeure());
        soin.setObservations(vide(request.observations()));
        if (request.prescriptionId() != null) {
            Prescription prescription = prescriptionRepository.findById(request.prescriptionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable"));
            if (!prescription.getConsultation().getRendezVous().getPatient().getId().equals(soin.getPatient().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette ordonnance concerne un autre patient");
            }
            soin.setPrescription(prescription);
            soin.setPrescripteurExterne(null);
        } else {
            soin.setPrescription(null);
            soin.setPrescripteurExterne(vide(request.prescripteurExterne()));
        }
    }

    private Patient patientDeLaDemande(SoinRequest request) {
        if (request.patientId() != null && request.nouveauPatient() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez un patient existant ou créez-en un, pas les deux");
        }
        if (request.nouveauPatient() != null) {
            return trouverPatient(patientService.creer(request.nouveauPatient()).id());
        }
        if (request.patientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un patient ou saisissez un nouveau patient");
        }
        return trouverPatient(request.patientId());
    }

    private Facture factureActive(Long soinId) {
        return factureRepository.findBySoinId(soinId).stream()
                .filter(facture -> facture.getStatut() != StatutFacture.ANNULEE)
                .findFirst().orElse(null);
    }

    private Soin trouverSoin(Long id) {
        return soinRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soin introuvable"));
    }

    private Patient trouverPatient(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
    }

    private static void refuser(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }

    private static String vide(String valeur) { return valeur == null || valeur.isBlank() ? null : valeur.trim(); }
}

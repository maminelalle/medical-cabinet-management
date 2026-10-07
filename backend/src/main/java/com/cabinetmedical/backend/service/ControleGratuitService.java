package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ControleGratuitResponse;
import com.cabinetmedical.backend.dto.RegleControleGratuitDto;
import com.cabinetmedical.backend.entity.ParametresCabinet;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutFacture;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.ParametresCabinetRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Consultation de controle gratuite : apres une consultation payee, le patient revient voir le meme medecin
 * sans payer, dans le delai et la limite fixes par la direction (par defaut 1 controle sous 30 jours).
 */
@Service
@RequiredArgsConstructor
public class ControleGratuitService {
    /** Consultations qui ouvrent le droit : le patient a ete vu (ou est en cours de consultation). */
    private static final Set<StatutRendezVous> CONSULTATIONS_VUES = EnumSet.of(StatutRendezVous.EN_COURS, StatutRendezVous.TERMINE);
    /** Controles qui ne consomment pas le droit. */
    private static final Set<StatutRendezVous> CONTROLES_PERDUS = EnumSet.of(StatutRendezVous.ANNULE, StatutRendezVous.ABSENT);

    private final ParametresCabinetRepository parametresRepository;
    private final RendezVousRepository rendezVousRepository;
    private final FactureRepository factureRepository;

    @Transactional(readOnly = true)
    public RegleControleGratuitDto regle() {
        return RegleControleGratuitDto.from(parametres());
    }

    @Transactional
    public RegleControleGratuitDto modifierRegle(RegleControleGratuitDto request) {
        ParametresCabinet parametres = parametresRepository.findById(ParametresCabinet.IDENTIFIANT).orElseGet(() -> {
            ParametresCabinet nouveaux = new ParametresCabinet();
            nouveaux.setNom("Cabinet Médical");
            return nouveaux;
        });
        parametres.setControleGratuitActif(request.actif());
        parametres.setControleGratuitJours(request.jours());
        parametres.setControleGratuitNombre(request.nombre());
        parametres.setUpdatedAt(Instant.now());
        return RegleControleGratuitDto.from(parametresRepository.save(parametres));
    }

    /** Recherche la consultation payee la plus recente qui ouvre un controle gratuit a cette date. */
    @Transactional(readOnly = true)
    public ControleGratuitResponse eligibilite(Long patientId, Long medecinId, LocalDateTime date, Long rendezVousExclu) {
        ParametresCabinet regle = parametres();
        if (!regle.isControleGratuitActif()) {
            return ControleGratuitResponse.nonEligible("Le contrôle gratuit est désactivé par la direction");
        }
        String dernierRefus = "Aucune consultation payée avec ce médecin dans les " + regle.getControleGratuitJours() + " derniers jours";
        for (RendezVous origine : rendezVousRepository.findByPatientIdOrderByDateHeureDesc(patientId)) {
            if (!origine.getMedecin().getId().equals(medecinId) || origine.getRendezVousOrigine() != null
                    || Objects.equals(origine.getId(), rendezVousExclu) || !origine.getDateHeure().isBefore(date)) continue;
            String refus = refus(origine, date, rendezVousExclu, true, regle);
            if (refus == null) return droit(origine, rendezVousExclu, regle);
            if (CONSULTATIONS_VUES.contains(origine.getStatut())) dernierRefus = refus;
            if (!dansLeDelai(origine, date, regle)) break; // les consultations suivantes sont encore plus anciennes
        }
        return ControleGratuitResponse.nonEligible(dernierRefus);
    }

    /**
     * Verifie qu'un rendez-vous peut etre le controle gratuit de {@code origine}. Le paiement de la consultation
     * d'origine est exige a la prise de rendez-vous par l'accueil ; quand le medecin programme le controle
     * en fin de consultation, il est verifie au moment de facturer le controle.
     */
    public void verifier(RendezVous origine, Long patientId, Long medecinId, LocalDateTime date, Long rendezVousExclu,
                         boolean exigerPaiement) {
        if (!origine.getPatient().getId().equals(patientId) || !origine.getMedecin().getId().equals(medecinId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le contrôle gratuit se fait avec le même patient et le même médecin");
        }
        String refus = refus(origine, date, rendezVousExclu, exigerPaiement, parametres());
        if (refus != null) throw new ResponseStatusException(HttpStatus.CONFLICT, refus);
    }

    /** Remonte a la consultation payee : le controle d'un controle reste rattache a la consultation initiale. */
    public RendezVous racine(RendezVous rendezVous) {
        return rendezVous.getRendezVousOrigine() == null ? rendezVous : rendezVous.getRendezVousOrigine();
    }

    /** Le rendez-vous est gratuit si la regle est active, la consultation d'origine payee et le delai respecte. */
    public boolean estGratuit(RendezVous rendezVous) {
        RendezVous origine = rendezVous.getRendezVousOrigine();
        if (origine == null) return false;
        ParametresCabinet regle = parametres();
        return regle.isControleGratuitActif() && dansLeDelai(origine, rendezVous.getDateHeure(), regle) && estPayee(origine);
    }

    /** Motif du refus, ou null si {@code origine} ouvre un controle gratuit a {@code date}. */
    public String refus(RendezVous origine, LocalDateTime date, Long rendezVousExclu, boolean exigerPaiement) {
        return refus(origine, date, rendezVousExclu, exigerPaiement, parametres());
    }

    private String refus(RendezVous origine, LocalDateTime date, Long rendezVousExclu, boolean exigerPaiement, ParametresCabinet regle) {
        if (!regle.isControleGratuitActif()) return "Le contrôle gratuit est désactivé par la direction";
        if (origine.getRendezVousOrigine() != null) return "Ce rendez-vous est lui-même un contrôle";
        if (!CONSULTATIONS_VUES.contains(origine.getStatut())) return "La consultation d'origine n'a pas eu lieu";
        if (!origine.getDateHeure().isBefore(date)) return "Le contrôle doit avoir lieu après la consultation d'origine";
        if (!dansLeDelai(origine, date, regle)) {
            return "Le délai de " + regle.getControleGratuitJours() + " jours après la consultation du "
                    + origine.getDateHeure().toLocalDate() + " est dépassé";
        }
        if (exigerPaiement && !estPayee(origine)) return "La consultation d'origine n'est pas encore payée";
        if (controlesUtilises(origine, rendezVousExclu) >= regle.getControleGratuitNombre()) {
            return "Le nombre de contrôles gratuits (" + regle.getControleGratuitNombre()
                    + ") est déjà atteint pour cette consultation";
        }
        return null;
    }

    private ControleGratuitResponse droit(RendezVous origine, Long rendezVousExclu, ParametresCabinet regle) {
        int restants = regle.getControleGratuitNombre() - (int) controlesUtilises(origine, rendezVousExclu);
        return new ControleGratuitResponse(true, origine.getId(), origine.getDateHeure(), dateLimite(origine, regle), restants,
                "Contrôle gratuit : consultation payée du " + origine.getDateHeure().toLocalDate());
    }

    private long controlesUtilises(RendezVous origine, Long rendezVousExclu) {
        return rendezVousRepository.findByRendezVousOrigineId(origine.getId()).stream()
                .filter(controle -> !controle.getId().equals(rendezVousExclu))
                .filter(controle -> !CONTROLES_PERDUS.contains(controle.getStatut()))
                .count();
    }

    private boolean estPayee(RendezVous origine) {
        return factureRepository.findByRendezVousId(origine.getId()).stream()
                .anyMatch(facture -> facture.getStatut() == StatutFacture.PAYEE
                        && facture.getMontantTotal().compareTo(BigDecimal.ZERO) > 0);
    }

    private static boolean dansLeDelai(RendezVous origine, LocalDateTime date, ParametresCabinet regle) {
        return !date.toLocalDate().isAfter(dateLimite(origine, regle));
    }

    private static LocalDate dateLimite(RendezVous origine, ParametresCabinet regle) {
        return origine.getDateHeure().toLocalDate().plusDays(regle.getControleGratuitJours());
    }

    private ParametresCabinet parametres() {
        return parametresRepository.findById(ParametresCabinet.IDENTIFIANT).orElseGet(ParametresCabinet::new);
    }
}

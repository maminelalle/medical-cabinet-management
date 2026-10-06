package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.admin.AdminDtos.PresenceMedecinResponse;
import com.cabinetmedical.backend.entity.Medecin;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.SessionUtilisateur;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.MedecinRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import com.cabinetmedical.backend.repository.SessionUtilisateurRepository;
import com.cabinetmedical.backend.security.JwtService;
import com.cabinetmedical.backend.security.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Presence des medecins au cabinet, calculee a partir de leurs sessions et de leur planning du jour :
 * EN_CONSULTATION (un rendez-vous en cours), EN_RETARD (un rendez-vous non commence depuis plus de 10 minutes),
 * DISPONIBLE (connecte, rien en cours) ou ABSENT (non connecte).
 */
@Service
@RequiredArgsConstructor
public class PresenceService {
    private static final Duration TOLERANCE_RETARD = Duration.ofMinutes(10);

    private final MedecinRepository medecinRepository;
    private final RendezVousRepository rendezVousRepository;
    private final SessionUtilisateurRepository sessionRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public List<PresenceMedecinResponse> presenceMedecins() {
        Instant maintenant = Instant.now();
        LocalDateTime heureCourante = LocalDateTime.now();
        LocalDate jour = LocalDate.now();
        Map<Long, Instant> activiteParUtilisateur = sessionRepository.findByDateFinIsNullOrderByDerniereActiviteDesc().stream()
                .filter(session -> session.getDateConnexion().plusMillis(jwtService.getExpirationMs()).isAfter(maintenant))
                .collect(Collectors.toMap(session -> session.getUtilisateur().getId(), SessionUtilisateur::getDerniereActivite,
                        (a, b) -> a.isAfter(b) ? a : b));
        Map<Long, List<RendezVous>> planning = rendezVousRepository
                .findByDateHeureGreaterThanEqualAndDateHeureLessThanOrderByDateHeureAsc(jour.atStartOfDay(), jour.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(rendezVous -> rendezVous.getMedecin().getId()));

        return medecinRepository.findAll().stream()
                .sorted(Comparator.comparing(Medecin::getNom))
                .map(medecin -> {
                    Instant activite = activiteParUtilisateur.get(medecin.getUtilisateur().getId());
                    boolean connecte = activite != null && activite.isAfter(maintenant.minus(SessionService.EN_LIGNE));
                    List<RendezVous> rendezVous = planning.getOrDefault(medecin.getId(), List.of());
                    RendezVous enCours = rendezVous.stream().filter(r -> r.getStatut() == StatutRendezVous.EN_COURS).findFirst().orElse(null);
                    List<RendezVous> aVenir = rendezVous.stream()
                            .filter(r -> r.getStatut() == StatutRendezVous.PLANIFIE || r.getStatut() == StatutRendezVous.CONFIRME).toList();
                    RendezVous enRetard = aVenir.stream()
                            .filter(r -> r.getDateHeure().plus(TOLERANCE_RETARD).isBefore(heureCourante)).findFirst().orElse(null);
                    RendezVous prochain = aVenir.stream().findFirst().orElse(null);
                    String statut = enCours != null ? "EN_CONSULTATION" : enRetard != null ? "EN_RETARD" : connecte ? "DISPONIBLE" : "ABSENT";
                    return new PresenceMedecinResponse(medecin.getId(), medecin.getNom(), medecin.getPrenom(), medecin.getSpecialite(),
                            connecte, activite, statut,
                            enCours == null ? null : enCours.getPatient().getPrenom() + " " + enCours.getPatient().getNom(),
                            enCours == null ? null : enCours.getDebutConsultation(),
                            prochain == null ? null : prochain.getDateHeure(),
                            prochain == null ? null : prochain.getPatient().getPrenom() + " " + prochain.getPatient().getNom(),
                            enRetard == null ? 0 : Duration.between(enRetard.getDateHeure(), heureCourante).toMinutes(),
                            aVenir.size(), rendezVous.stream().filter(r -> r.getStatut() == StatutRendezVous.TERMINE).count());
                }).toList();
    }
}

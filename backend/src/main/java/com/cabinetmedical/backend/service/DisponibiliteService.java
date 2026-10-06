package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.CreneauResponse;
import com.cabinetmedical.backend.entity.ActeProgramme;
import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutActe;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.repository.ActeProgrammeRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Occupation de l'agenda d'un medecin : rendez-vous et actes programmes.
 * Un creneau reserve ne peut pas etre attribue a un autre patient tant que le premier n'est pas annule.
 */
@Service
@RequiredArgsConstructor
public class DisponibiliteService {
    public static final LocalTime OUVERTURE = LocalTime.of(8, 0);
    public static final LocalTime FERMETURE = LocalTime.of(18, 0);
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final RendezVousRepository rendezVousRepository;
    private final ActeProgrammeRepository acteProgrammeRepository;

    /** Leve un 409 si [debut, debut + duree[ chevauche une occupation du medecin (hors elements ignores). */
    @Transactional(readOnly = true)
    public void verifierDisponible(Long medecinId, LocalDateTime debut, int dureeMinutes,
                                   Long rendezVousIgnore, Long acteIgnore) {
        LocalDateTime fin = debut.plusMinutes(dureeMinutes);
        occupations(medecinId, debut.toLocalDate()).stream()
                .filter(occupation -> !(occupation.rendezVous() && Objects.equals(occupation.id(), rendezVousIgnore)))
                .filter(occupation -> !(!occupation.rendezVous() && Objects.equals(occupation.id(), acteIgnore)))
                .filter(occupation -> occupation.debut().isBefore(fin) && debut.isBefore(occupation.fin()))
                .findFirst()
                .ifPresent(occupation -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                            "Créneau déjà réservé : le médecin a %s de %s à %s. Choisissez un autre horaire ou annulez d'abord cette réservation.",
                            occupation.rendezVous() ? "un rendez-vous" : "un acte programmé",
                            occupation.debut().format(HEURE), occupation.fin().format(HEURE)));
                });
    }

    /** Grille de la journee (08:00 - 18:00) par pas de la duree demandee, avec l'etat de chaque creneau. */
    @Transactional(readOnly = true)
    public List<CreneauResponse> creneaux(Long medecinId, LocalDate jour, int dureeMinutes) {
        List<Occupation> occupations = occupations(medecinId, jour);
        LocalDateTime maintenant = LocalDateTime.now();
        List<CreneauResponse> creneaux = new ArrayList<>();
        for (LocalDateTime debut = jour.atTime(OUVERTURE); !debut.plusMinutes(dureeMinutes).isAfter(jour.atTime(FERMETURE));
             debut = debut.plusMinutes(dureeMinutes)) {
            LocalDateTime fin = debut.plusMinutes(dureeMinutes);
            LocalDateTime debutCreneau = debut;
            Occupation occupation = occupations.stream()
                    .filter(item -> item.debut().isBefore(fin) && debutCreneau.isBefore(item.fin()))
                    .findFirst().orElse(null);
            creneaux.add(new CreneauResponse(debut.format(HEURE), fin.format(HEURE), occupation == null && debut.isAfter(maintenant),
                    debut.isBefore(maintenant), occupation == null ? null : occupation.libelle()));
        }
        return creneaux;
    }

    private List<Occupation> occupations(Long medecinId, LocalDate jour) {
        LocalDateTime debutJour = jour.atStartOfDay();
        LocalDateTime finJour = jour.plusDays(1).atStartOfDay();
        List<Occupation> occupations = new ArrayList<>();
        // Un rendez-vous annule ou absent libere son creneau.
        for (RendezVous rendezVous : rendezVousRepository
                .findByMedecinIdAndDateHeureGreaterThanEqualAndDateHeureLessThanOrderByDateHeureAsc(medecinId, debutJour.minusHours(8), finJour)) {
            if (rendezVous.getStatut() == StatutRendezVous.ANNULE || rendezVous.getStatut() == StatutRendezVous.ABSENT) continue;
            occupations.add(new Occupation(true, rendezVous.getId(), rendezVous.getDateHeure(),
                    rendezVous.getDateHeure().plusMinutes(rendezVous.getDureeMinutes()),
                    "Rendez-vous · " + rendezVous.getPatient().getPrenom() + " " + rendezVous.getPatient().getNom()));
        }
        for (ActeProgramme acte : acteProgrammeRepository
                .findByMedecinIdAndStatutAndDateHeureBetween(medecinId, StatutActe.PLANIFIE, debutJour.minusDays(1), finJour)) {
            occupations.add(new Occupation(false, acte.getId(), acte.getDateHeure(),
                    acte.getDateHeure().plusMinutes(acte.getDureeMinutes()), "Acte · " + acte.getIntitule()));
        }
        return occupations;
    }

    private record Occupation(boolean rendezVous, Long id, LocalDateTime debut, LocalDateTime fin, String libelle) {}
}

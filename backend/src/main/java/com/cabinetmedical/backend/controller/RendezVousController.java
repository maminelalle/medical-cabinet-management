package com.cabinetmedical.backend.controller;

import com.cabinetmedical.backend.dto.ControleGratuitResponse;
import com.cabinetmedical.backend.dto.ControleRequest;
import com.cabinetmedical.backend.dto.CreneauResponse;
import com.cabinetmedical.backend.dto.RendezVousRequest;
import com.cabinetmedical.backend.dto.RendezVousResponse;
import com.cabinetmedical.backend.dto.StatutRendezVousRequest;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import com.cabinetmedical.backend.service.RendezVousService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rendezvous")
@RequiredArgsConstructor
public class RendezVousController {
    private final RendezVousService rendezVousService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<RendezVousResponse> rechercher(
            @RequestParam(required = false) Long medecinId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutRendezVous statut,
            @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.rechercher(medecinId, date, dateDebut, dateFin, statut, utilisateurConnecte);
    }

    /** Grille des creneaux d'un medecin pour une journee (libres, passes, occupes). */
    @GetMapping("/creneaux")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public List<CreneauResponse> creneaux(@RequestParam Long medecinId, @RequestParam LocalDate date,
                                          @RequestParam(required = false) Integer dureeMinutes) {
        return rendezVousService.creneaux(medecinId, date, dureeMinutes);
    }

    /** Le patient a-t-il droit a un controle gratuit avec ce medecin a cette date ? */
    @GetMapping("/controle-gratuit")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public ControleGratuitResponse controleGratuit(@RequestParam Long patientId, @RequestParam Long medecinId,
                                                   @RequestParam LocalDateTime date,
                                                   @RequestParam(required = false) Long exclure) {
        return rendezVousService.controleGratuit(patientId, medecinId, date, exclure);
    }

    /** Le medecin programme le rendez-vous de controle du patient a la fin de la consultation. */
    @PostMapping("/{id}/controle")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEDECIN')")
    public RendezVousResponse programmerControle(@PathVariable Long id, @Valid @RequestBody ControleRequest request,
                                                 @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.programmerControle(id, request, utilisateurConnecte);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACCUEIL')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        rendezVousService.supprimer(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN')")
    public RendezVousResponse trouver(@PathVariable Long id) {
        return rendezVousService.trouverRendezVous(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ACCUEIL')")
    public RendezVousResponse creer(@Valid @RequestBody RendezVousRequest request,
                                    @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.creer(request, utilisateurConnecte);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACCUEIL')")
    public RendezVousResponse modifier(@PathVariable Long id, @Valid @RequestBody RendezVousRequest request) {
        return rendezVousService.modifier(id, request);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ACCUEIL', 'MEDECIN')")
    public RendezVousResponse changerStatut(@PathVariable Long id,
                                            @Valid @RequestBody StatutRendezVousRequest request,
                                            @AuthenticationPrincipal UserDetails utilisateurConnecte) {
        return rendezVousService.changerStatut(id, request.statut(), utilisateurConnecte);
    }
}
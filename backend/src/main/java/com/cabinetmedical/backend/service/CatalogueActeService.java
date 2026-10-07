package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.CatalogueActeRequest;
import com.cabinetmedical.backend.dto.CatalogueActeResponse;
import com.cabinetmedical.backend.entity.CatalogueActe;
import com.cabinetmedical.backend.repository.CatalogueActeRepository;
import com.cabinetmedical.backend.repository.LigneFactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Grille tarifaire du cabinet, geree par la direction. */
@Service
@RequiredArgsConstructor
public class CatalogueActeService {
    private final CatalogueActeRepository repository;
    private final LigneFactureRepository ligneFactureRepository;

    /** Tarifs proposes a la facturation (actifs) ; {@code tous} = grille complete pour la page Tarifs. */
    @Transactional(readOnly = true)
    public List<CatalogueActeResponse> lister(boolean tous) {
        return repository.findAllByOrderByTypeAscLibelleAsc().stream()
                .filter(acte -> tous || acte.isActif())
                .map(acte -> CatalogueActeResponse.from(acte, tous && ligneFactureRepository.existsByCatalogueActeId(acte.getId())))
                .toList();
    }

    @Transactional
    public CatalogueActeResponse creer(CatalogueActeRequest request) {
        CatalogueActe acte = new CatalogueActe();
        appliquer(acte, request);
        return CatalogueActeResponse.from(repository.save(acte));
    }

    @Transactional
    public CatalogueActeResponse modifier(Long id, CatalogueActeRequest request) {
        CatalogueActe acte = trouver(id);
        appliquer(acte, request);
        return CatalogueActeResponse.from(repository.save(acte), ligneFactureRepository.existsByCatalogueActeId(id));
    }

    /** Un tarif deja facture reste dans l'historique : il se desactive au lieu d'etre supprime. */
    @Transactional
    public void supprimer(Long id) {
        CatalogueActe acte = trouver(id);
        if (ligneFactureRepository.existsByCatalogueActeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce tarif figure déjà sur des factures : désactivez-le au lieu de le supprimer");
        }
        repository.delete(acte);
    }

    private static void appliquer(CatalogueActe acte, CatalogueActeRequest request) {
        acte.setLibelle(request.libelle().trim());
        acte.setType(request.type().trim().toUpperCase());
        acte.setMontantDefaut(request.montantDefaut());
        acte.setSpecialite(vide(request.specialite()));
        acte.setDescription(vide(request.description()));
        if (request.actif() != null) acte.setActif(request.actif());
    }

    private CatalogueActe trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarif introuvable"));
    }

    private static String vide(String valeur) { return valeur == null || valeur.isBlank() ? null : valeur.trim(); }
}

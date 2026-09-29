package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.CatalogueActeRequest;
import com.cabinetmedical.backend.dto.CatalogueActeResponse;
import com.cabinetmedical.backend.entity.CatalogueActe;
import com.cabinetmedical.backend.repository.CatalogueActeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogueActeService {
    private final CatalogueActeRepository repository;

    @Transactional(readOnly = true)
    public List<CatalogueActeResponse> lister() {
        return repository.findAll().stream().filter(CatalogueActe::isActif).map(CatalogueActeResponse::from).toList();
    }

    @Transactional
    public CatalogueActeResponse creer(CatalogueActeRequest request) {
        CatalogueActe acte = new CatalogueActe();
        acte.setLibelle(request.libelle());
        acte.setType(request.type().toUpperCase());
        acte.setMontantDefaut(request.montantDefaut());
        return CatalogueActeResponse.from(repository.save(acte));
    }
}
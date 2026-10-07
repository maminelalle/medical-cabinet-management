package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.LigneFacture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {
	List<LigneFacture> findByFactureId(Long factureId);

	boolean existsByCatalogueActeId(Long catalogueActeId);
}

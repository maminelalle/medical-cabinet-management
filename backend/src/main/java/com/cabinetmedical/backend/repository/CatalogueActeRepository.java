package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.CatalogueActe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogueActeRepository extends JpaRepository<CatalogueActe, Long> {
	List<CatalogueActe> findAllByOrderByTypeAscLibelleAsc();
}

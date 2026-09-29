package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
	List<Paiement> findByFactureIdOrderByDatePaiementDesc(Long factureId);
	@Query("select coalesce(sum(p.montant), 0) from Paiement p where p.facture.id = :factureId")
	BigDecimal sumMontantByFactureId(@Param("factureId") Long factureId);
}
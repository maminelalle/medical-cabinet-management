package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactureRepository extends JpaRepository<Facture, Long> {
	List<Facture> findAllByOrderByDateFactureDesc();

	List<Facture> findByPatientIdOrderByDateFactureDesc(Long patientId);

	long countByPatientId(Long patientId);

	List<Facture> findByRendezVousId(Long rendezVousId);

	List<Facture> findByActeProgrammeId(Long acteProgrammeId);

	List<Facture> findBySoinId(Long soinId);
}
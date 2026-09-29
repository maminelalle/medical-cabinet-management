package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
	boolean existsByRendezVousId(Long rendezVousId);

	@Query("select c from Consultation c join fetch c.rendezVous r join fetch r.patient join fetch r.medecin "
			+ "where r.patient.id = :patientId order by r.dateHeure desc")
	List<Consultation> findByPatientIdOrderByDateHeureDesc(@Param("patientId") Long patientId);
}
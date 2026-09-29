package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
	boolean existsByConsultationId(Long consultationId);

	Optional<Prescription> findByConsultationId(Long consultationId);
}
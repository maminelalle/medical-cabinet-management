package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
	boolean existsByConsultationId(Long consultationId);

	Optional<Prescription> findByConsultationId(Long consultationId);

	List<Prescription> findAllByOrderByDatePrescriptionDesc();
}
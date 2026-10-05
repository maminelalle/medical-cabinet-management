package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispensationRepository extends JpaRepository<Dispensation, Long> {
    boolean existsByPrescriptionId(Long prescriptionId);
}
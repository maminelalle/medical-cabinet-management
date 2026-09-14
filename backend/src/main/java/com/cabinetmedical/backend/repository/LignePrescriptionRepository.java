package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.LignePrescription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LignePrescriptionRepository extends JpaRepository<LignePrescription, Long> {}
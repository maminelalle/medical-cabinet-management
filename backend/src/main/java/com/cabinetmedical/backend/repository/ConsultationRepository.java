package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {}
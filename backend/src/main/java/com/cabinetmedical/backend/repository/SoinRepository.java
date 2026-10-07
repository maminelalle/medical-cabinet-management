package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Soin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SoinRepository extends JpaRepository<Soin, Long> {
    List<Soin> findAllByOrderByDateHeureDesc();

    List<Soin> findByPatientIdOrderByDateHeureDesc(Long patientId);
}

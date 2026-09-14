package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {}
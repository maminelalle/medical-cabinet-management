package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DispensationRepository extends JpaRepository<Dispensation, Long> {
    boolean existsByPrescriptionId(Long prescriptionId);

    Optional<Dispensation> findByFactureId(Long factureId);

    long countByDateDispensationGreaterThanEqualAndDateDispensationLessThan(Instant debut, Instant fin);

    List<Dispensation> findByDateDispensationGreaterThanEqualAndDateDispensationLessThan(Instant debut, Instant fin);
}

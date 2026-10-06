package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.MouvementStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    List<MouvementStock> findByMedicamentIdOrderByDateMouvementDescIdDesc(Long medicamentId);

    List<MouvementStock> findByDateMouvementGreaterThanEqualAndDateMouvementLessThanOrderByDateMouvementDescIdDesc(
            Instant debut, Instant fin);
}

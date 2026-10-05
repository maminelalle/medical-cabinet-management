package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicamentRepository extends JpaRepository<Medicament, Long> {
    List<Medicament> findAllByActifTrueOrderByNomAsc();
}
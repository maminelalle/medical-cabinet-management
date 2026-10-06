package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.ActeProgramme;
import com.cabinetmedical.backend.entity.StatutActe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActeProgrammeRepository extends JpaRepository<ActeProgramme, Long> {
    List<ActeProgramme> findAllByOrderByDateHeureDesc();

    List<ActeProgramme> findByMedecinIdOrderByDateHeureDesc(Long medecinId);

    List<ActeProgramme> findByPatientIdOrderByDateHeureDesc(Long patientId);

    List<ActeProgramme> findByMedecinIdAndStatutAndDateHeureBetween(Long medecinId, StatutActe statut,
                                                                  LocalDateTime debut, LocalDateTime fin);
}

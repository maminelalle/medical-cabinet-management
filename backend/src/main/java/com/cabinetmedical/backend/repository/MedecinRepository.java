package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Medecin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedecinRepository extends JpaRepository<Medecin, Long> {
    Optional<Medecin> findByUtilisateurId(Long utilisateurId);
}
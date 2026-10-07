package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.SessionUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SessionUtilisateurRepository extends JpaRepository<SessionUtilisateur, Long> {
    Optional<SessionUtilisateur> findByJetonId(String jetonId);

    List<SessionUtilisateur> findByDateFinIsNullOrderByDerniereActiviteDesc();

    List<SessionUtilisateur> findByUtilisateurIdAndDateFinIsNull(Long utilisateurId);

    List<SessionUtilisateur> findByUtilisateurId(Long utilisateurId);

    List<SessionUtilisateur> findByDateConnexionGreaterThanEqualOrderByDateConnexionDesc(Instant depuis);
}

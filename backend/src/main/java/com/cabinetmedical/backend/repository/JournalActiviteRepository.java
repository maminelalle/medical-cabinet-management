package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.JournalActivite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {
    List<JournalActivite> findByDateActionGreaterThanEqualOrderByDateActionDesc(Instant depuis, Pageable page);

    List<JournalActivite> findByUtilisateurIdAndDateActionGreaterThanEqualOrderByDateActionDesc(Long utilisateurId,
                                                                                               Instant depuis, Pageable page);

    long countByDateActionGreaterThanEqual(Instant depuis);
}

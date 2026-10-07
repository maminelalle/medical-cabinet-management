package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.JournalActivite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long> {
    List<JournalActivite> findByDateActionGreaterThanEqualOrderByDateActionDesc(Instant depuis, Pageable page);

    List<JournalActivite> findByUtilisateurIdAndDateActionGreaterThanEqualOrderByDateActionDesc(Long utilisateurId,
                                                                                               Instant depuis, Pageable page);

    long countByDateActionGreaterThanEqual(Instant depuis);

    /** Un compte supprime garde son historique : l'entree conserve l'email, le lien vers le compte est retire. */
    @Modifying
    @Query("update JournalActivite j set j.utilisateur = null where j.utilisateur.id = :utilisateurId")
    void detacherUtilisateur(@Param("utilisateurId") Long utilisateurId);
}

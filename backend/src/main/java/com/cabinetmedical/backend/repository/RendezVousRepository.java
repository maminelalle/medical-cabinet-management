package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
		List<RendezVous> findAllByOrderByDateHeureAsc();

		List<RendezVous> findByPatientIdOrderByDateHeureDesc(Long patientId);

		long countByPatientId(Long patientId);

		java.util.Optional<RendezVous> findFirstByPatientIdAndMedecinIdAndDateHeure(Long patientId, Long medecinId,
				java.time.LocalDateTime dateHeure);

		boolean existsByMedecinIdAndDateHeureAndStatutNot(Long medecinId, java.time.LocalDateTime dateHeure,
																											StatutRendezVous statut);

		long countByDateHeureGreaterThanEqualAndDateHeureLessThan(java.time.LocalDateTime debut,
				java.time.LocalDateTime fin);
}
package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.RendezVous;
import com.cabinetmedical.backend.entity.StatutRendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
		List<RendezVous> findAllByOrderByDateHeureAsc();

		List<RendezVous> findByPatientIdOrderByDateHeureDesc(Long patientId);

		List<RendezVous> findByRendezVousOrigineId(Long rendezVousOrigineId);

		long countByPatientId(Long patientId);

		List<RendezVous> findByMedecinIdAndDateHeureGreaterThanEqualAndDateHeureLessThanOrderByDateHeureAsc(Long medecinId,
				java.time.LocalDateTime debut, java.time.LocalDateTime fin);

		List<RendezVous> findByDateHeureGreaterThanEqualAndDateHeureLessThanOrderByDateHeureAsc(java.time.LocalDateTime debut,
				java.time.LocalDateTime fin);

		java.util.Optional<RendezVous> findFirstByPatientIdAndMedecinIdAndDateHeure(Long patientId, Long medecinId,
				java.time.LocalDateTime dateHeure);

		boolean existsByMedecinIdAndDateHeureAndStatutNot(Long medecinId, java.time.LocalDateTime dateHeure,
																											StatutRendezVous statut);

		long countByDateHeureGreaterThanEqualAndDateHeureLessThan(java.time.LocalDateTime debut,
				java.time.LocalDateTime fin);
}

package com.cabinetmedical.backend.repository;

import com.cabinetmedical.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(String nom, String prenom);

    Optional<Patient> findFirstByNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissance(String nom, String prenom,
                                                                                LocalDate dateNaissance);
}

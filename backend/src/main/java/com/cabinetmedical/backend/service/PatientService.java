package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.PatientRequest;
import com.cabinetmedical.backend.dto.PatientResponse;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<PatientResponse> rechercher(String query) {
        List<Patient> patients = query == null || query.isBlank()
                ? patientRepository.findAll()
                : patientRepository.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(query, query);
        return patients.stream().map(PatientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse trouver(Long id) {
        return PatientResponse.from(patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable")));
    }

    @Transactional
    public PatientResponse creer(PatientRequest request) {
        return PatientResponse.from(patientRepository.save(remplir(new Patient(), request)));
    }

    @Transactional
    public PatientResponse modifier(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
        return PatientResponse.from(patientRepository.save(remplir(patient, request)));
    }

    @Transactional
    public void supprimer(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable");
        }
        patientRepository.deleteById(id);
    }

    private Patient remplir(Patient patient, PatientRequest request) {
        patient.setNom(request.nom());
        patient.setPrenom(request.prenom());
        patient.setDateNaissance(request.dateNaissance());
        patient.setTelephone(request.telephone());
        patient.setEmail(request.email());
        patient.setAdresse(request.adresse());
        return patient;
    }
}

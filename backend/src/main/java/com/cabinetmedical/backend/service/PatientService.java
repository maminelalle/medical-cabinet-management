package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.PatientRequest;
import com.cabinetmedical.backend.dto.PatientResponse;
import com.cabinetmedical.backend.entity.Patient;
import com.cabinetmedical.backend.repository.FactureRepository;
import com.cabinetmedical.backend.repository.PatientRepository;
import com.cabinetmedical.backend.repository.RendezVousRepository;
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
    private final RendezVousRepository rendezVousRepository;
    private final FactureRepository factureRepository;

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
        // Un dossier qui contient un historique medical ou financier ne doit jamais etre efface.
        long rendezVous = rendezVousRepository.countByPatientId(id);
        long factures = factureRepository.countByPatientId(id);
        if (rendezVous > 0 || factures > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "Suppression impossible : ce patient a un historique (%d rendez-vous, %d facture(s)). Son dossier doit être conservé.",
                    rendezVous, factures));
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

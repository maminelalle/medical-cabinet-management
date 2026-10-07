package com.cabinetmedical.backend.service;

import com.cabinetmedical.backend.dto.ApparenceDto;
import com.cabinetmedical.backend.dto.ParametresCabinetDto;
import com.cabinetmedical.backend.entity.ParametresCabinet;
import com.cabinetmedical.backend.repository.ParametresCabinetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ParametresCabinetService {
    private final ParametresCabinetRepository repository;

    @Transactional(readOnly = true)
    public ParametresCabinetDto lire() {
        return repository.findById(ParametresCabinet.IDENTIFIANT).map(ParametresCabinetDto::from)
                .orElse(new ParametresCabinetDto("Cabinet Médical", "Cabinet de groupe", null, null, null));
    }

    @Transactional
    public ParametresCabinetDto modifier(ParametresCabinetDto request) {
        ParametresCabinet parametres = repository.findById(ParametresCabinet.IDENTIFIANT).orElseGet(ParametresCabinet::new);
        parametres.setNom(request.nom().trim());
        parametres.setSousTitre(vide(request.sousTitre()));
        parametres.setAdresse(vide(request.adresse()));
        parametres.setTelephone(vide(request.telephone()));
        parametres.setEmail(vide(request.email()));
        parametres.setUpdatedAt(Instant.now());
        return ParametresCabinetDto.from(repository.save(parametres));
    }

    /** Apparence de l'interface (lue aussi par la page de connexion, avant authentification). */
    @Transactional(readOnly = true)
    public ApparenceDto lireApparence() {
        return repository.findById(ParametresCabinet.IDENTIFIANT).map(ApparenceDto::from).orElseGet(() -> {
            ParametresCabinet defaut = new ParametresCabinet();
            defaut.setSousTitre("Cabinet de groupe");
            return ApparenceDto.from(defaut);
        });
    }

    @Transactional
    public ApparenceDto modifierApparence(ApparenceDto request) {
        ParametresCabinet parametres = repository.findById(ParametresCabinet.IDENTIFIANT).orElseGet(() -> {
            ParametresCabinet nouveaux = new ParametresCabinet();
            nouveaux.setNom("Cabinet Médical");
            return nouveaux;
        });
        parametres.setNomInterface(request.nomInterface().trim());
        parametres.setSousTitre(vide(request.sousTitre()));
        parametres.setCouleurPrincipale(request.couleurPrincipale().toLowerCase());
        parametres.setCouleurAccent(request.couleurAccent().toLowerCase());
        parametres.setCouleurBouton(request.couleurBouton().toLowerCase());
        parametres.setLogo(vide(request.logo()));
        parametres.setUpdatedAt(Instant.now());
        return ApparenceDto.from(repository.save(parametres));
    }

    private static String vide(String valeur) { return valeur == null || valeur.isBlank() ? null : valeur.trim(); }
}

package com.cabinetmedical.backend.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Reponse HTTP d'un document PDF telecharge. */
final class Pdf {
    private Pdf() {}

    static ResponseEntity<byte[]> reponse(byte[] contenu, String nomFichier) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(contenu);
    }
}

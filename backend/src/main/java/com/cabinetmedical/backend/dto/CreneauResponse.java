package com.cabinetmedical.backend.dto;

/** Creneau de l'agenda d'un medecin : libre, passe ou occupe (avec ce qui l'occupe). */
public record CreneauResponse(String debut, String fin, boolean libre, boolean passe, String occupePar) {}

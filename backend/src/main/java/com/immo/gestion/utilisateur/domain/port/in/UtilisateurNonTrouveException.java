package com.immo.gestion.utilisateur.domain.port.in;

public class UtilisateurNonTrouveException extends RuntimeException {

    public UtilisateurNonTrouveException() {
        super("Utilisateur introuvable.");
    }
}

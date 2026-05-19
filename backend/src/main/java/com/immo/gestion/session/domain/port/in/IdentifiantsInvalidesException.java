package com.immo.gestion.session.domain.port.in;

/**
 * Exception unique exposée à la couche web pour tous les échecs de connexion
 * (D7) : email inconnu, mot de passe invalide, compte inactif. Le motif détaillé
 * reste dans l'event d'audit et ne fuite pas vers l'API.
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("identifiants invalides");
    }
}

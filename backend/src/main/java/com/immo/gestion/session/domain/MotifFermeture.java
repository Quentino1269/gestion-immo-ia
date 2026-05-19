package com.immo.gestion.session.domain;

/**
 * Cf. docs/slices/authentification.md §9 (event {@code UtilisateurDeconnecte}).
 * {@code REVOQUEE} est réservée aux slices consommateurs (D10) : force-logout
 * suite à changement de mot de passe ou suspension de compte.
 */
public enum MotifFermeture {
    VOLONTAIRE,
    EXPIRATION,
    REVOQUEE
}

package com.immo.gestion.session.domain;

import java.util.Objects;

/**
 * Token de session opaque transmis au client une seule fois en clair.
 * Wrapper anti-fuite : pas de {@code toString}, pas de sérialisation.
 * Cf. docs/slices/authentification.md D2 et I-4 (≥ 128 bits d'entropie).
 */
public final class TokenSession {

    private final String valeur;

    public TokenSession(String valeur) {
        Objects.requireNonNull(valeur, "token requis");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException("token vide");
        }
        this.valeur = valeur;
    }

    public String valeur() {
        return valeur;
    }

    @Override
    public String toString() {
        return "[TokenSession *** masqué ***]";
    }
}
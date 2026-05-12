package com.immo.gestion.shared;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object email — normalisé (lowercase + trim) à la construction.
 * Cf. docs/slices/creation-utilisateur.md (D12, I-1).
 */
public record Email(String valeur) {

    private static final Pattern FORMAT = Pattern.compile(
            "^[^\\s@]{1,64}@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final int LONGUEUR_MAX = 254;

    public Email {
        Objects.requireNonNull(valeur, "email requis");
        valeur = valeur.trim().toLowerCase();
        if (valeur.isEmpty()) {
            throw new IllegalArgumentException("email vide");
        }
        if (valeur.length() > LONGUEUR_MAX) {
            throw new IllegalArgumentException("email trop long");
        }
        if (!FORMAT.matcher(valeur).matches()) {
            throw new IllegalArgumentException("email mal formé");
        }
    }
}

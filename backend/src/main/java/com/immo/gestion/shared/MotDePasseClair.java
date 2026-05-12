package com.immo.gestion.shared;

import java.util.Objects;

/**
 * Mot de passe en clair. Type dédié pour empêcher toute fuite accidentelle
 * (toString masqué, jamais sérialisé). Cf. docs/slices/creation-utilisateur.md §11.
 * <p>
 * Politique : longueur ≥ 12 caractères (I-4, NIST-style).
 */
public final class MotDePasseClair {

    public static final int LONGUEUR_MIN = 12;

    private final char[] valeur;

    public MotDePasseClair(String valeur) {
        Objects.requireNonNull(valeur, "mot de passe requis");
        if (valeur.length() < LONGUEUR_MIN) {
            throw new IllegalArgumentException(
                    "mot de passe trop court (min " + LONGUEUR_MIN + " caractères)"
            );
        }
        this.valeur = valeur.toCharArray();
    }

    /** Accès aux caractères pour le hashage, à n'utiliser qu'au sein du domaine. */
    public char[] caracteres() {
        return valeur;
    }

    @Override
    public String toString() {
        return "[MotDePasseClair *** masqué ***]";
    }
}

package com.immo.gestion.shared;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mot de passe soumis lors d'une tentative de connexion. Distinct de
 * {@link MotDePasseClair} car la politique de longueur ne s'applique
 * qu'à la création (slice creation-utilisateur, I-4). À la connexion,
 * une chaîne de n'importe quelle longueur peut être tentée.
 * <p>
 * Wrapper anti-fuite : pas de {@code toString}, pas de sérialisation,
 * effacement explicite via {@link #effacer()} après usage.
 */
public final class MotDePasseSoumis {

    private final char[] valeur;

    public MotDePasseSoumis(String valeur) {
        Objects.requireNonNull(valeur, "mot de passe soumis requis");
        this.valeur = valeur.toCharArray();
    }

    public char[] caracteres() {
        return valeur;
    }

    public void effacer() {
        Arrays.fill(valeur, '\0');
    }

    @Override
    public String toString() {
        return "[MotDePasseSoumis *** masqué ***]";
    }
}
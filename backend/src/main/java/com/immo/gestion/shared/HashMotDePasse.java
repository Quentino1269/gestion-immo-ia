package com.immo.gestion.shared;

import java.util.Objects;

/**
 * Empreinte d'un mot de passe au format PHC string (argon2id encoded).
 * Encapsule algo + paramètres + sel + hash. Cf. docs/slices/creation-utilisateur.md (D8).
 */
public record HashMotDePasse(String phcEncoded) {

    public HashMotDePasse {
        Objects.requireNonNull(phcEncoded, "hash requis");
        if (phcEncoded.isBlank()) {
            throw new IllegalArgumentException("hash vide");
        }
        if (!phcEncoded.startsWith("$argon2id$")) {
            throw new IllegalArgumentException("hash non argon2id");
        }
    }
}

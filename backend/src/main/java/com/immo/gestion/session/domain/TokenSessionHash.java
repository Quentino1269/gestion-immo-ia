package com.immo.gestion.session.domain;

import java.util.Objects;

/**
 * Empreinte SHA-256 (hex minuscule, 64 caractères) du {@link TokenSession}.
 * Seule représentation conservée côté serveur (D2).
 */
public record TokenSessionHash(String hex) {

    private static final int LONGUEUR_HEX_SHA256 = 64;

    public TokenSessionHash {
        Objects.requireNonNull(hex, "hash requis");
        if (hex.length() != LONGUEUR_HEX_SHA256) {
            throw new IllegalArgumentException("hash sha-256 attendu (64 hex)");
        }
        if (!hex.equals(hex.toLowerCase()) || !hex.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException("hash sha-256 non hex");
        }
    }
}

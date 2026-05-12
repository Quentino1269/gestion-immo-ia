package com.immo.gestion.utilisateur.domain;

import java.util.Objects;
import java.util.UUID;

public record UtilisateurId(UUID valeur) {

    public UtilisateurId {
        Objects.requireNonNull(valeur, "utilisateurId requis");
    }

    public static UtilisateurId nouveau() {
        return new UtilisateurId(UUID.randomUUID());
    }
}

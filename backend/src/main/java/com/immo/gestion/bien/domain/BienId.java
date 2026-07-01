package com.immo.gestion.bien.domain;

import java.util.Objects;
import java.util.UUID;

public record BienId(UUID valeur) {

    public BienId {
        Objects.requireNonNull(valeur, "bienId requis");
    }

    public static BienId nouveau() {
        return new BienId(UUID.randomUUID());
    }
}

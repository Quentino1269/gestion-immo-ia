package com.immo.gestion.session.domain;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID valeur) {

    public SessionId {
        Objects.requireNonNull(valeur, "sessionId requis");
    }

    public static SessionId nouveau() {
        return new SessionId(UUID.randomUUID());
    }
}
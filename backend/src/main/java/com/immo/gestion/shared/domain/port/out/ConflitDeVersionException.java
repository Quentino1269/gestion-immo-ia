package com.immo.gestion.shared.domain.port.out;

import java.util.UUID;

/**
 * Levée quand l'append d'événements sur un flux échoue faute d'accord sur la version attendue
 * (contrôle de concurrence optimiste, contrainte unique (stream_id, version) en base).
 */
public class ConflitDeVersionException extends RuntimeException {

    public ConflitDeVersionException(UUID streamId, long expectedVersion, Throwable cause) {
        super("Conflit de version sur le flux " + streamId + " (version attendue : " + expectedVersion + ")", cause);
    }
}

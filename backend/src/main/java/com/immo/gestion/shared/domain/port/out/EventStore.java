package com.immo.gestion.shared.domain.port.out;

import com.immo.gestion.shared.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

/**
 * Port d'accès au log d'événements append-only (source de vérité des aggregates).
 * Cf. MISSION.md §5 (Event Sourcing strict).
 */
public interface EventStore {

    /**
     * Ajoute de nouveaux événements au flux {@code streamId}. {@code expectedVersion} est la
     * version connue au chargement (0 si le flux n'existe pas encore) ; en cas de désaccord avec
     * la version réellement en base, lève {@link ConflitDeVersionException}.
     */
    void append(UUID streamId, String streamType, long expectedVersion, List<DomainEvent> nouveauxEvenements);

    /**
     * Charge tous les événements du flux, dans l'ordre chronologique. Liste vide si le flux
     * n'existe pas.
     */
    List<DomainEvent> charger(UUID streamId);
}

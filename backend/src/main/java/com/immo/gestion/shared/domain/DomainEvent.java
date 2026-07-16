package com.immo.gestion.shared.domain;

import java.time.Instant;

/**
 * Marqueur commun à tout événement domaine destiné à l'event store.
 * Cf. MISSION.md §5 (Event Sourcing strict).
 */
public interface DomainEvent {

    Instant survenuLe();
}

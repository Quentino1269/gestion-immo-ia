package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Suppression d'une simulation de rentabilité — append-only comme le reste du bounded context
 * (rien n'est jamais effacé de l'event store ; seule la projection de lecture est retirée).
 * Cf. docs/slices/suppression-simulation-rentabilite.md §7.
 */
public record SimulationRentabiliteSupprimee(
        SimulationRentabiliteId simulationId,
        Instant survenuLe
) implements DomainEvent {
}

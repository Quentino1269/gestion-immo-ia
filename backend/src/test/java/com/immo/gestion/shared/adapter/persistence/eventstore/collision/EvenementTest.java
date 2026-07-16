package com.immo.gestion.shared.adapter.persistence.eventstore.collision;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Même simple name que {@code DomainEventTypeRegistryTest.EvenementTest}, package différent —
 * sert uniquement à tester la détection de collision de {@link
 * com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry#enregistrer}.
 */
public record EvenementTest(Instant survenuLe) implements DomainEvent {
}

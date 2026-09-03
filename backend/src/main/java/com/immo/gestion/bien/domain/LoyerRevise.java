package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Révision du loyer hors charges d'un bien déjà créé.
 * Cf. docs/slices/modification-bien.md §8.
 */
public record LoyerRevise(
        BienId bienId,
        long loyerHorsChargesEnCentimes,
        Instant survenuLe
) implements DomainEvent {
}

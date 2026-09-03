package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Révision du montant de charges d'un bien déjà créé.
 * Cf. docs/slices/modification-bien.md §8.
 */
public record ChargesRevisees(
        BienId bienId,
        long chargesEnCentimes,
        Instant survenuLe
) implements DomainEvent {
}

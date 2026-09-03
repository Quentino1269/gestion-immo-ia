package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Révision de la date de disponibilité d'un bien déjà créé.
 * Cf. docs/slices/modification-bien.md §8.
 */
public record DisponibiliteRevisee(
        BienId bienId,
        LocalDate disponibleAPartirDu,
        Instant survenuLe
) implements DomainEvent {
}

package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Renommage du libellé d'une chambre en colocation déjà créée.
 * Cf. docs/slices/modification-bien.md §8.
 */
public record LibelleChambreRenomme(
        BienId bienId,
        String libelleChambre,
        Instant survenuLe
) implements DomainEvent {
}

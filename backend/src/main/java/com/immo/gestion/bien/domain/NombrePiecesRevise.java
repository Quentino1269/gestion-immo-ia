package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Révision du nombre de pièces principales d'un bien déjà créé.
 * Cf. docs/slices/modification-bien.md §8.
 */
public record NombrePiecesRevise(
        BienId bienId,
        int nbPiecesPrincipales,
        Instant survenuLe
) implements DomainEvent {
}

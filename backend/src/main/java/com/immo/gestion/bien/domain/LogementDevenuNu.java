package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * Bascule d'un bien vers le statut nu (non meublé). {@code modaliteCharges} est dérivée
 * (toujours {@code PROVISION}) et historisée pour traçabilité, comme à la création (I-CHARGES-1).
 * Cf. docs/slices/modification-bien.md §8.
 */
public record LogementDevenuNu(
        BienId bienId,
        ModaliteCharges modaliteCharges,
        Instant survenuLe
) implements DomainEvent {
}

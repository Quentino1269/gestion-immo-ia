package com.immo.gestion.session.domain;

import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;

/**
 * Évènement émis au succès de {@code SeConnecter} (slice authentification §9).
 * Le token de session en clair n'apparaît pas ici (D2 : jamais conservé) ; seul son
 * {@code tokenHash} est porté, nécessaire pour rejouer l'aggregate {@link Session} (Event Sourcing).
 */
public record UtilisateurConnecte(
        SessionId sessionId,
        UtilisateurId utilisateurId,
        TokenSessionHash tokenHash,
        Instant expireA,
        String userAgent,
        String ipSource,
        Instant survenuLe
) implements DomainEvent {
}

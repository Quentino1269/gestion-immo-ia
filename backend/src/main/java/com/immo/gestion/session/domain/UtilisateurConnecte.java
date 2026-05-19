package com.immo.gestion.session.domain;

import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;

/**
 * Évènement émis au succès de {@code SeConnecter} (slice authentification §9).
 * Le token de session n'apparaît pas ici (D2 : jamais conservé en clair) ;
 * seul le {@code tokenHash} est associé à la session via {@link Session}.
 */
public record UtilisateurConnecte(
        SessionId sessionId,
        UtilisateurId utilisateurId,
        Instant expireA,
        String userAgent,
        String ipSource,
        Instant survenuLe
) {
}

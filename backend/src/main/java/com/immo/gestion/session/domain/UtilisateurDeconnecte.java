package com.immo.gestion.session.domain;

import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;

/**
 * Évènement émis à la fermeture d'une session (slice authentification §9) :
 * logout volontaire, expiration ou révocation (D10).
 */
public record UtilisateurDeconnecte(
        SessionId sessionId,
        UtilisateurId utilisateurId,
        MotifFermeture motif,
        Instant survenuLe
) {
}

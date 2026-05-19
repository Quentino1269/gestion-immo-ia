package com.immo.gestion.session.domain.port.in;

import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TokenSession;

import java.time.Instant;

/**
 * Renvoyé au succès de {@code SeConnecter}. Le {@link TokenSession}
 * en clair ne transite ici qu'une seule fois, vers le client (D2).
 */
public record SeConnecterResultat(
        SessionId sessionId,
        TokenSession tokenClair,
        Instant expireA
) {
}

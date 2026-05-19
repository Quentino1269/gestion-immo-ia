package com.immo.gestion.session.adapter.web;

import java.time.Instant;
import java.util.UUID;

/**
 * Réponse du endpoint de login : le {@code token} y figure en clair pour la
 * seule et unique fois (D2). Aucun hash, aucune information de cookie.
 */
public record SeConnecterResponse(UUID sessionId, String token, Instant expireA) {
}

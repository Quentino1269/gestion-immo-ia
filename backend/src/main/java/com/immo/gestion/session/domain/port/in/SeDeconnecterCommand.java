package com.immo.gestion.session.domain.port.in;

import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

/**
 * Cf. docs/slices/authentification.md §8.2.
 * {@code utilisateurAppelant} provient du contexte d'authentification résolu
 * par {@link com.immo.gestion.session.adapter.web.BearerAuthFilter} ; sert à
 * vérifier I-6 (un utilisateur ne peut fermer qu'une session lui appartenant).
 */
public record SeDeconnecterCommand(SessionId sessionId, UtilisateurId utilisateurAppelant) {
}

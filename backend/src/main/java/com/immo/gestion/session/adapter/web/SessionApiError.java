package com.immo.gestion.session.adapter.web;

/**
 * Forme stable et minimaliste des erreurs renvoyées par {@link SessionController}.
 * Volontairement séparée de {@code com.immo.gestion.utilisateur.adapter.web.ApiError}
 * pour ne porter qu'un message générique (D7).
 */
public record SessionApiError(String message) {
}

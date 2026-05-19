package com.immo.gestion.session.adapter.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload du endpoint de login. Validation minimale (présence) ; les
 * vérifications métier (format email, etc.) restent côté domaine pour
 * préserver le timing constant (D7).
 */
public record SeConnecterRequest(
        @NotBlank String email,
        @NotBlank String motDePasse
) {
}

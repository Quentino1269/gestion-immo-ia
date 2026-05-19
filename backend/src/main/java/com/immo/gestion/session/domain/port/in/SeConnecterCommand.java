package com.immo.gestion.session.domain.port.in;

import com.immo.gestion.shared.MotDePasseSoumis;

/**
 * Cf. docs/slices/authentification.md §8.1.
 * Le {@link MotDePasseSoumis} n'est jamais persisté ni loggué (I-8).
 */
public record SeConnecterCommand(
        String email,
        MotDePasseSoumis motDePasseSoumis,
        String userAgent,
        String ipSource
) {
}

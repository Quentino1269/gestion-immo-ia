package com.immo.gestion.session.domain;

/**
 * Évènement d'audit émis à chaque échec de connexion (D7, D9).
 * Le {@link RaisonEchecConnexion} est conservé côté serveur uniquement —
 * il ne fuite pas dans la réponse API.
 */
public record TentativeDeConnexionEchouee(
        String emailSoumis,
        RaisonEchecConnexion raison,
        String userAgent,
        String ipSource,
        java.time.Instant survenuLe
) {
}

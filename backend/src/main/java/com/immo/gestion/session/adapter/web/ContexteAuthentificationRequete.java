package com.immo.gestion.session.adapter.web;

import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Clés et helpers pour exposer le résultat du {@link BearerAuthFilter} aux
 * couches en aval (controllers). On ne dépend pas de Spring Security en V1 ;
 * le filter pose deux attributs de requête, les controllers les relisent.
 */
public final class ContexteAuthentificationRequete {

    public static final String ATTR_UTILISATEUR_ID = "session.utilisateurId";
    public static final String ATTR_SESSION_ID = "session.sessionId";

    private ContexteAuthentificationRequete() {
    }

    public static Optional<UtilisateurId> utilisateurCourant(HttpServletRequest requete) {
        Object valeur = requete.getAttribute(ATTR_UTILISATEUR_ID);
        if (valeur instanceof UUID id) {
            return Optional.of(new UtilisateurId(id));
        }
        return Optional.empty();
    }

    public static Optional<SessionId> sessionCourante(HttpServletRequest requete) {
        Object valeur = requete.getAttribute(ATTR_SESSION_ID);
        if (valeur instanceof UUID id) {
            return Optional.of(new SessionId(id));
        }
        return Optional.empty();
    }
}

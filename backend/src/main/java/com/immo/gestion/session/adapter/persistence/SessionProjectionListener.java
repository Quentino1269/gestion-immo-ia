package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.EtatSession;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.UtilisateurConnecte;
import com.immo.gestion.session.domain.UtilisateurDeconnecte;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Maintient la projection {@code sessions} à jour à partir des événements de l'event store.
 * {@code @EventListener} volontairement synchrone (pas {@code @TransactionalEventListener}) pour
 * s'exécuter dans la même transaction que l'append. Cf. MISSION.md §5.
 */
@Component
public class SessionProjectionListener {

    private final SessionJpaRepository jpa;

    public SessionProjectionListener(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @EventListener
    public void surConnexion(UtilisateurConnecte evenement) {
        jpa.save(new SessionEntity(
                evenement.sessionId().valeur(),
                evenement.utilisateurId().valeur(),
                evenement.tokenHash().hex(),
                evenement.survenuLe(),
                evenement.expireA(),
                EtatSession.ACTIVE,
                null,
                null,
                evenement.userAgent(),
                evenement.ipSource()
        ));
    }

    @EventListener
    public void surDeconnexion(UtilisateurDeconnecte evenement) {
        SessionEntity entite = charger(evenement.sessionId());
        entite.setEtat(EtatSession.FERMEE);
        entite.setMotifFermeture(evenement.motif());
        entite.setFermeeLe(evenement.survenuLe());
        jpa.save(entite);
    }

    private SessionEntity charger(SessionId id) {
        return jpa.findById(id.valeur())
                .orElseThrow(() -> new IllegalStateException(
                        "Projection sessions incohérente : session introuvable " + id));
    }
}

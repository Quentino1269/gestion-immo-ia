package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.TentativeDeConnexionEchouee;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TentativeDeConnexionEchoueeListener {

    private final TentativeDeConnexionEchoueeJpaRepository jpa;

    public TentativeDeConnexionEchoueeListener(TentativeDeConnexionEchoueeJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * {@code REQUIRES_NEW} : l'audit doit survivre même quand la transaction appelante
     * (ex. {@code SessionService.seConnecter}) est annulée par l'exception d'échec de
     * connexion levée juste après la publication de cet événement — sinon la ligne
     * d'audit disparaît avec le rollback et le journal anti-brute-force reste vide.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void surEchec(TentativeDeConnexionEchouee evenement) {
        jpa.save(new TentativeDeConnexionEchoueeEntity(
                evenement.emailSoumis(),
                evenement.raison(),
                evenement.userAgent(),
                evenement.ipSource(),
                evenement.survenuLe()
        ));
    }
}

package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.shared.Adresse;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Maintient la projection {@code biens} à jour à partir des événements de l'event store.
 * {@code @EventListener} volontairement synchrone (pas {@code @TransactionalEventListener}) pour
 * s'exécuter dans la même transaction que l'append. Cf. MISSION.md §5.
 */
@Component
public class BienProjectionListener {

    private final BienJpaRepository jpa;

    public BienProjectionListener(BienJpaRepository jpa) {
        this.jpa = jpa;
    }

    @EventListener
    public void surBienAjoute(BienAjouteAuPortefeuille evenement) {
        Adresse adresse = evenement.adresse();
        jpa.save(new BienEntity(
                evenement.bienId().valeur(),
                evenement.proprietaireInitialId().valeur(),
                evenement.typeBien(),
                evenement.bienParentId() != null ? evenement.bienParentId().valeur() : null,
                evenement.libelleChambre(),
                evenement.nbPiecesPrincipales(),
                evenement.surfaceM2(),
                evenement.meuble(),
                evenement.loyerHorsChargesEnCentimes(),
                evenement.chargesEnCentimes(),
                evenement.modaliteCharges(),
                adresse.numero(),
                adresse.voie(),
                adresse.complement(),
                adresse.codePostal(),
                adresse.commune(),
                adresse.paysIso(),
                evenement.disponibleAPartirDu(),
                evenement.survenuLe()
        ));
    }
}

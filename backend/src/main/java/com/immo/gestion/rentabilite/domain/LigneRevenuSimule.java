package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;

import java.util.Objects;

/**
 * Une ligne de revenu locatif simulé, rattachée au bien racine lui-même (pas de colocation)
 * ou à l'une de ses chambres actives (D4, D5). Cf. §7 "Revenus locatifs simulés".
 */
public record LigneRevenuSimule(
        BienId bienSourceId,
        long loyerSimuleMensuelEnCentimes,
        long chargesSimuleesMensuellesEnCentimes
) {

    public LigneRevenuSimule {
        Objects.requireNonNull(bienSourceId, "bienSourceId requis");
        // I-SIM-12
        if (loyerSimuleMensuelEnCentimes < 0) {
            throw new IllegalArgumentException("loyerSimuleMensuelEnCentimes doit être ≥ 0");
        }
        if (chargesSimuleesMensuellesEnCentimes < 0) {
            throw new IllegalArgumentException("chargesSimuleesMensuellesEnCentimes doit être ≥ 0");
        }
    }
}

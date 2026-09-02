package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Cf. docs/slices/projection-rentabilite.md §7 "Hypothèses d'évolution", D12. */
public record HypothesesEvolution(
        BigDecimal tauxVacanceLocativePourcent,
        BigDecimal tauxIndexationLoyerPourcent,
        BigDecimal tauxIndexationChargesPourcent
) {

    public HypothesesEvolution {
        Objects.requireNonNull(tauxVacanceLocativePourcent, "tauxVacanceLocativePourcent requis");
        Objects.requireNonNull(tauxIndexationLoyerPourcent, "tauxIndexationLoyerPourcent requis");
        Objects.requireNonNull(tauxIndexationChargesPourcent, "tauxIndexationChargesPourcent requis");
        // I-SIM-13
        if (tauxVacanceLocativePourcent.signum() < 0
                || tauxVacanceLocativePourcent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException("tauxVacanceLocativePourcent doit être dans [0,100)");
        }
        // I-SIM-14 : pas de déflation modélisée en V1.
        if (tauxIndexationLoyerPourcent.signum() < 0) {
            throw new IllegalArgumentException("tauxIndexationLoyerPourcent doit être ≥ 0");
        }
        if (tauxIndexationChargesPourcent.signum() < 0) {
            throw new IllegalArgumentException("tauxIndexationChargesPourcent doit être ≥ 0");
        }
    }
}

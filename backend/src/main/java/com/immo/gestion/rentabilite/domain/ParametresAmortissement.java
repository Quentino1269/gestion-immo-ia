package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Décomposition du coût d'acquisition pour l'amortissement LMNP (D10). N'a d'effet que si
 * {@code regimeFiscal = REEL_BIC} (I-SIM-16) ; les contraintes de forme restent vérifiées
 * dans tous les cas pour ne jamais transporter une donnée absurde.
 */
public record ParametresAmortissement(
        BigDecimal quotePartTerrainPourcent,
        BigDecimal quotePartMobilierPourcent,
        int dureeAmortissementBatiAnnees,
        int dureeAmortissementMobilierAnnees
) {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    public ParametresAmortissement {
        Objects.requireNonNull(quotePartTerrainPourcent, "quotePartTerrainPourcent requis");
        Objects.requireNonNull(quotePartMobilierPourcent, "quotePartMobilierPourcent requis");
        verifierPourcent(quotePartTerrainPourcent, "quotePartTerrainPourcent");
        verifierPourcent(quotePartMobilierPourcent, "quotePartMobilierPourcent");
        // I-SIM-10
        if (quotePartTerrainPourcent.add(quotePartMobilierPourcent).compareTo(CENT) > 0) {
            throw new IllegalArgumentException("quotePartTerrain + quotePartMobilier doit être ≤ 100");
        }
        if (dureeAmortissementBatiAnnees < 1) {
            throw new IllegalArgumentException("dureeAmortissementBatiAnnees doit être ≥ 1");
        }
        if (dureeAmortissementMobilierAnnees < 1) {
            throw new IllegalArgumentException("dureeAmortissementMobilierAnnees doit être ≥ 1");
        }
    }

    private static void verifierPourcent(BigDecimal valeur, String nom) {
        if (valeur.signum() < 0 || valeur.compareTo(CENT) > 0) {
            throw new IllegalArgumentException(nom + " doit être dans [0,100]");
        }
    }

    public BigDecimal quotePartBatiPourcent() {
        return CENT.subtract(quotePartTerrainPourcent).subtract(quotePartMobilierPourcent);
    }
}

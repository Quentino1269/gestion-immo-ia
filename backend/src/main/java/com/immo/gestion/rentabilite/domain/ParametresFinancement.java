package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cf. docs/slices/projection-rentabilite.md §7 "Financement", D8.
 * {@code montantEmprunteEnCentimes = 0} désigne un achat cash (I-SIM-7) : dans ce cas,
 * {@code tauxAnnuelPourcent}/{@code dureeAnnees} sont forcés à zéro (I-SIM-8), quels que
 * soient les valeurs soumises.
 */
public record ParametresFinancement(
        long montantEmprunteEnCentimes,
        BigDecimal tauxAnnuelPourcent,
        int dureeAnnees,
        BigDecimal tauxAssuranceEmprunteurPourcent
) {

    public ParametresFinancement {
        if (montantEmprunteEnCentimes < 0) {
            throw new IllegalArgumentException("montantEmprunteEnCentimes doit être ≥ 0");
        }
        Objects.requireNonNull(tauxAssuranceEmprunteurPourcent, "tauxAssuranceEmprunteurPourcent requis");
        if (tauxAssuranceEmprunteurPourcent.signum() < 0) {
            throw new IllegalArgumentException("tauxAssuranceEmprunteurPourcent doit être ≥ 0");
        }

        if (montantEmprunteEnCentimes == 0) {
            // I-SIM-8 : achat cash, financement ignoré.
            tauxAnnuelPourcent = BigDecimal.ZERO;
            dureeAnnees = 0;
        } else {
            Objects.requireNonNull(tauxAnnuelPourcent, "tauxAnnuelPourcent requis si montantEmprunte > 0");
            if (tauxAnnuelPourcent.signum() < 0) {
                throw new IllegalArgumentException("tauxAnnuelPourcent doit être ≥ 0");
            }
            if (dureeAnnees < 1) {
                throw new IllegalArgumentException("dureeAnnees doit être ≥ 1 si montantEmprunte > 0");
            }
        }
    }

    public boolean estCash() {
        return montantEmprunteEnCentimes == 0;
    }
}

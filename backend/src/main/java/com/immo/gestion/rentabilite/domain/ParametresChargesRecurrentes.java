package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Cf. docs/slices/projection-rentabilite.md §7 "Charges récurrentes annuelles". */
public record ParametresChargesRecurrentes(
        long taxeFonciereEnCentimes,
        long assurancePnoEnCentimes,
        long assuranceLoyersImpayesEnCentimes,
        BigDecimal fraisGestionLocativePourcentLoyer,
        long provisionTravauxAnnuelleEnCentimes,
        long fraisComptabiliteAnnuelEnCentimes,
        long chargesCoproprieteNonRecuperablesEnCentimes
) {

    public ParametresChargesRecurrentes {
        if (taxeFonciereEnCentimes < 0 || assurancePnoEnCentimes < 0 || assuranceLoyersImpayesEnCentimes < 0
                || provisionTravauxAnnuelleEnCentimes < 0 || fraisComptabiliteAnnuelEnCentimes < 0
                || chargesCoproprieteNonRecuperablesEnCentimes < 0) {
            throw new IllegalArgumentException("les charges récurrentes doivent être ≥ 0");
        }
        Objects.requireNonNull(fraisGestionLocativePourcentLoyer, "fraisGestionLocativePourcentLoyer requis");
        if (fraisGestionLocativePourcentLoyer.signum() < 0
                || fraisGestionLocativePourcentLoyer.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("fraisGestionLocativePourcentLoyer doit être dans [0,100]");
        }
    }

    /** Somme des postes fixes (hors gestion locative, proportionnelle au loyer). */
    public long chargesFixesEnCentimes() {
        return taxeFonciereEnCentimes + assurancePnoEnCentimes + assuranceLoyersImpayesEnCentimes
                + chargesCoproprieteNonRecuperablesEnCentimes + provisionTravauxAnnuelleEnCentimes
                + fraisComptabiliteAnnuelEnCentimes;
    }
}

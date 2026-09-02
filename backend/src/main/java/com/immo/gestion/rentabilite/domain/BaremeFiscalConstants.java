package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;

/**
 * Taux et seuils légaux (D17, D19) — une seule version « courante », pas d'historisation en V1.
 * Valeurs de référence 2026, à revalider/mettre à jour manuellement à chaque loi de finances.
 */
public final class BaremeFiscalConstants {

    /** Abattement forfaitaire micro-foncier (location nue). */
    public static final BigDecimal ABATTEMENT_MICRO_FONCIER = new BigDecimal("0.30");

    /** Abattement forfaitaire micro-BIC (location meublée). */
    public static final BigDecimal ABATTEMENT_MICRO_BIC = new BigDecimal("0.50");

    /** Plafond annuel de déficit foncier imputable sur le revenu global (régime réel foncier, D11). */
    public static final long PLAFOND_DEFICIT_FONCIER_IMPUTABLE_REVENU_GLOBAL_EN_CENTIMES = 10_700_00L;

    /** Taux de prélèvements sociaux (CSG/CRDS) sur les revenus locatifs nets. */
    public static final BigDecimal TAUX_PRELEVEMENTS_SOCIAUX = new BigDecimal("0.172");

    private BaremeFiscalConstants() {
    }
}

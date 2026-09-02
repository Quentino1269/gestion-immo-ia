package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;

/**
 * Une année de la projection (§8, §11). Champs figés au calcul, jamais recalculés (D2).
 * Les champs propres à un régime non applicable valent {@code 0} (I-SIM-15, I-SIM-16, I-SIM-18).
 */
public record LigneProjection(
        int annee,
        long loyerBrutAnnuelEnCentimes,
        long chargesNonRecuperablesAnnuellesEnCentimes,
        long interetsEmpruntAnnuelsEnCentimes,
        long capitalRembourseAnnuelEnCentimes,
        long assuranceEmprunteurAnnuelleEnCentimes,
        long capitalRestantDuFinAnneeEnCentimes,
        long amortissementBatiAnnuelEnCentimes,
        long amortissementMobilierAnnuelEnCentimes,
        long resultatImposableEnCentimes,
        long deficitReportableUtiliseEnCentimes,
        long soldeDeficitFoncierReportableFinAnneeEnCentimes,
        long soldeDeficitBicReportableFinAnneeEnCentimes,
        long impotEstimeEnCentimes,
        long cashFlowAvantFinancementAvantImpotEnCentimes,
        long cashFlowApresFinancementAvantImpotEnCentimes,
        long cashFlowApresFinancementApresImpotEnCentimes,
        BigDecimal rendementBrutPourcent,
        BigDecimal rendementNetPourcent,
        BigDecimal rendementNetNetPourcent,
        BigDecimal rendementSurFondsPropresPourcent
) {
}

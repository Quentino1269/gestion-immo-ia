package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcule la projection année par année d'une simulation de rentabilité. Fonction pure,
 * sans effet de bord ni dépendance externe. Cf. docs/slices/projection-rentabilite.md §11.
 */
public final class ProjectionCalculateur {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal DOUZE = BigDecimal.valueOf(12);
    private static final MathContext MC = new MathContext(30, RoundingMode.HALF_UP);

    private ProjectionCalculateur() {
    }

    public static List<LigneProjection> calculer(
            RegimeFiscal regimeFiscal,
            int tmiFoyerPourcent,
            int horizonAnnees,
            ParametresAcquisition acquisition,
            ParametresFinancement financement,
            ParametresAmortissement amortissement,
            List<LigneRevenuSimule> revenusLocatifsSimules,
            ParametresChargesRecurrentes chargesRecurrentes,
            HypothesesEvolution hypothesesEvolution
    ) {
        // I-SIM-5, vérifié ici (et pas seulement dans le constructeur de SimulationRentabilite, appelé
        // après ce calcul) car horizonAnnees dimensionne directement les tableaux ci-dessous : un appel
        // avec une valeur hors bornes doit être refusé proprement plutôt que lever une NegativeArraySizeException.
        if (horizonAnnees < 1 || horizonAnnees > 40) {
            throw new IllegalArgumentException("horizonAnnees doit être dans [1,40]");
        }

        long coutTotalAcquisitionEnCentimes = acquisition.coutTotalEnCentimes();
        long apportPersonnelEnCentimes = coutTotalAcquisitionEnCentimes - financement.montantEmprunteEnCentimes();

        TableauAmortissement tableau = calculerTableauAmortissement(financement, horizonAnnees);

        BigDecimal facteurLoyer = BigDecimal.ONE.add(
                hypothesesEvolution.tauxIndexationLoyerPourcent().divide(CENT, MC));
        BigDecimal facteurCharges = BigDecimal.ONE.add(
                hypothesesEvolution.tauxIndexationChargesPourcent().divide(CENT, MC));
        BigDecimal unMoinsVacance = BigDecimal.ONE.subtract(
                hypothesesEvolution.tauxVacanceLocativePourcent().divide(CENT, MC));

        BigDecimal sommeLoyersMensuels = revenusLocatifsSimules.stream()
                .map(l -> BigDecimal.valueOf(l.loyerSimuleMensuelEnCentimes()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal chargesFixesBase = BigDecimal.valueOf(chargesRecurrentes.chargesFixesEnCentimes());

        BigDecimal coutTotal = BigDecimal.valueOf(coutTotalAcquisitionEnCentimes);
        BigDecimal baseAmortissableBati = coutTotal.multiply(amortissement.quotePartBatiPourcent()).divide(CENT, MC);
        BigDecimal baseAmortissableMobilier = coutTotal.multiply(amortissement.quotePartMobilierPourcent()).divide(CENT, MC);
        BigDecimal amortissementBatiAnnuelMontant =
                baseAmortissableBati.divide(BigDecimal.valueOf(amortissement.dureeAmortissementBatiAnnees()), MC);
        BigDecimal amortissementMobilierAnnuelMontant =
                baseAmortissableMobilier.divide(BigDecimal.valueOf(amortissement.dureeAmortissementMobilierAnnees()), MC);

        BigDecimal tmiFraction = BigDecimal.valueOf(tmiFoyerPourcent).divide(CENT, MC);

        BigDecimal soldeDeficitFoncier = BigDecimal.ZERO;
        BigDecimal soldeDeficitBic = BigDecimal.ZERO;

        List<LigneProjection> lignes = new ArrayList<>(horizonAnnees);

        for (int n = 1; n <= horizonAnnees; n++) {
            BigDecimal facteurIndexLoyer = facteurLoyer.pow(n - 1, MC);
            BigDecimal facteurIndexCharges = facteurCharges.pow(n - 1, MC);

            BigDecimal loyerBrut = sommeLoyersMensuels.multiply(DOUZE, MC)
                    .multiply(facteurIndexLoyer, MC)
                    .multiply(unMoinsVacance, MC);

            BigDecimal fraisGestion = loyerBrut.multiply(chargesRecurrentes.fraisGestionLocativePourcentLoyer()).divide(CENT, MC);
            BigDecimal chargesNonRecuperables = chargesFixesBase.multiply(facteurIndexCharges, MC).add(fraisGestion);

            long interetsAnnuels = tableau.interets()[n - 1];
            long capitalRembourse = tableau.capitalRembourse()[n - 1];
            long assuranceAnnuelle = tableau.assurance()[n - 1];
            long capitalRestantDuFin = tableau.capitalRestantDu()[n - 1];

            BigDecimal amortissementBati = (regimeFiscal == RegimeFiscal.REEL_BIC && n <= amortissement.dureeAmortissementBatiAnnees())
                    ? amortissementBatiAnnuelMontant : BigDecimal.ZERO;
            BigDecimal amortissementMobilier = (regimeFiscal == RegimeFiscal.REEL_BIC && n <= amortissement.dureeAmortissementMobilierAnnees())
                    ? amortissementMobilierAnnuelMontant : BigDecimal.ZERO;

            BigDecimal resultatImposable;
            BigDecimal deficitImputeRevenuGlobal = BigDecimal.ZERO;

            switch (regimeFiscal) {
                case MICRO_FONCIER -> resultatImposable =
                        loyerBrut.multiply(BigDecimal.ONE.subtract(BaremeFiscalConstants.ABATTEMENT_MICRO_FONCIER));
                case MICRO_BIC -> resultatImposable =
                        loyerBrut.multiply(BigDecimal.ONE.subtract(BaremeFiscalConstants.ABATTEMENT_MICRO_BIC));
                case REEL_FONCIER -> {
                    BigDecimal chargesDeductibles = chargesNonRecuperables
                            .add(BigDecimal.valueOf(interetsAnnuels))
                            .add(BigDecimal.valueOf(assuranceAnnuelle));
                    BigDecimal resultatBrut = loyerBrut.subtract(chargesDeductibles);
                    ResultatImputationDeficit imputation = imputerDeficit(resultatBrut, soldeDeficitFoncier,
                            BigDecimal.valueOf(BaremeFiscalConstants.PLAFOND_DEFICIT_FONCIER_IMPUTABLE_REVENU_GLOBAL_EN_CENTIMES));
                    resultatImposable = imputation.resultatImposable();
                    deficitImputeRevenuGlobal = imputation.deficitImputeRevenuGlobal();
                    soldeDeficitFoncier = imputation.nouveauSolde();
                }
                case REEL_BIC -> {
                    BigDecimal chargesDeductibles = chargesNonRecuperables
                            .add(BigDecimal.valueOf(interetsAnnuels))
                            .add(BigDecimal.valueOf(assuranceAnnuelle))
                            .add(amortissementBati)
                            .add(amortissementMobilier);
                    BigDecimal resultatBrutBic = loyerBrut.subtract(chargesDeductibles);
                    // Pas de plafond : le déficit BIC n'est jamais imputable sur le revenu global (D21).
                    ResultatImputationDeficit imputation = imputerDeficit(resultatBrutBic, soldeDeficitBic, null);
                    resultatImposable = imputation.resultatImposable();
                    soldeDeficitBic = imputation.nouveauSolde();
                }
                default -> throw new IllegalStateException("régime fiscal non géré : " + regimeFiscal);
            }

            BigDecimal impotEstime = resultatImposable.multiply(
                    tmiFraction.add(BaremeFiscalConstants.TAUX_PRELEVEMENTS_SOCIAUX), MC);
            if (regimeFiscal == RegimeFiscal.REEL_FONCIER) {
                impotEstime = impotEstime.subtract(deficitImputeRevenuGlobal.multiply(tmiFraction, MC));
            }

            BigDecimal cashFlowAvantFinancementAvantImpot = loyerBrut.subtract(chargesNonRecuperables);
            BigDecimal cashFlowApresFinancementAvantImpot = cashFlowAvantFinancementAvantImpot
                    .subtract(BigDecimal.valueOf(interetsAnnuels))
                    .subtract(BigDecimal.valueOf(capitalRembourse))
                    .subtract(BigDecimal.valueOf(assuranceAnnuelle));
            BigDecimal cashFlowApresFinancementApresImpot = cashFlowApresFinancementAvantImpot.subtract(impotEstime);

            BigDecimal rendementBrut = loyerBrut.divide(coutTotal, MC).multiply(CENT);
            BigDecimal rendementNet = cashFlowAvantFinancementAvantImpot.divide(coutTotal, MC).multiply(CENT);
            BigDecimal rendementNetNet = cashFlowAvantFinancementAvantImpot.subtract(impotEstime)
                    .divide(coutTotal, MC).multiply(CENT);
            BigDecimal rendementFondsPropres = apportPersonnelEnCentimes == 0 ? null
                    : cashFlowApresFinancementApresImpot.divide(BigDecimal.valueOf(apportPersonnelEnCentimes), MC).multiply(CENT);

            lignes.add(new LigneProjection(
                    n,
                    versCentimes(loyerBrut),
                    versCentimes(chargesNonRecuperables),
                    interetsAnnuels,
                    capitalRembourse,
                    assuranceAnnuelle,
                    capitalRestantDuFin,
                    versCentimes(amortissementBati),
                    versCentimes(amortissementMobilier),
                    versCentimes(resultatImposable),
                    versCentimes(deficitImputeRevenuGlobal),
                    regimeFiscal == RegimeFiscal.REEL_FONCIER ? versCentimes(soldeDeficitFoncier) : 0L,
                    regimeFiscal == RegimeFiscal.REEL_BIC ? versCentimes(soldeDeficitBic) : 0L,
                    versCentimes(impotEstime),
                    versCentimes(cashFlowAvantFinancementAvantImpot),
                    versCentimes(cashFlowApresFinancementAvantImpot),
                    versCentimes(cashFlowApresFinancementApresImpot),
                    arrondiPourcent(rendementBrut),
                    arrondiPourcent(rendementNet),
                    arrondiPourcent(rendementNetNet),
                    rendementFondsPropres != null ? arrondiPourcent(rendementFondsPropres) : null
            ));
        }

        return lignes;
    }

    /** Tableau d'amortissement du prêt, agrégé par année (D8). Tout à zéro si achat cash. */
    private static TableauAmortissement calculerTableauAmortissement(ParametresFinancement financement, int horizonAnnees) {
        long[] interetsParAnnee = new long[horizonAnnees];
        long[] capitalRembourseParAnnee = new long[horizonAnnees];
        long[] capitalRestantDuFinAnnee = new long[horizonAnnees];
        long[] assuranceParAnnee = new long[horizonAnnees];

        if (financement.estCash()) {
            return new TableauAmortissement(interetsParAnnee, capitalRembourseParAnnee, capitalRestantDuFinAnnee, assuranceParAnnee);
        }

        BigDecimal montant = BigDecimal.valueOf(financement.montantEmprunteEnCentimes());
        BigDecimal tauxMensuel = financement.tauxAnnuelPourcent().divide(CENT.multiply(DOUZE), MC);
        int dureeMois = financement.dureeAnnees() * 12;

        BigDecimal mensualite;
        if (tauxMensuel.signum() == 0) {
            mensualite = montant.divide(BigDecimal.valueOf(dureeMois), 0, RoundingMode.HALF_UP);
        } else {
            BigDecimal facteur = BigDecimal.ONE.add(tauxMensuel).pow(dureeMois, MC);
            BigDecimal numerateur = montant.multiply(tauxMensuel, MC).multiply(facteur, MC);
            BigDecimal denominateur = facteur.subtract(BigDecimal.ONE);
            mensualite = numerateur.divide(denominateur, 0, RoundingMode.HALF_UP);
        }

        BigDecimal assuranceAnnuelleMontant = versCentimesEnBigDecimal(
                montant.multiply(financement.tauxAssuranceEmprunteurPourcent()).divide(CENT, MC));

        int moisACalculer = Math.min(dureeMois, horizonAnnees * 12);
        BigDecimal capitalRestant = montant;
        long interetsAnneeCourante = 0;
        long principalAnneeCourante = 0;
        int anneeCourante = 1;

        for (int m = 1; m <= moisACalculer; m++) {
            BigDecimal interetMoisExact = capitalRestant.multiply(tauxMensuel, MC);
            long interetMoisCentimes = versCentimes(interetMoisExact);
            long principalMoisCentimes = versCentimes(mensualite.subtract(interetMoisExact));
            long capitalRestantCentimes = capitalRestant.longValueExact();
            if (m == dureeMois || principalMoisCentimes > capitalRestantCentimes) {
                principalMoisCentimes = capitalRestantCentimes;
            }
            capitalRestant = capitalRestant.subtract(BigDecimal.valueOf(principalMoisCentimes));

            interetsAnneeCourante += interetMoisCentimes;
            principalAnneeCourante += principalMoisCentimes;

            if (m % 12 == 0) {
                interetsParAnnee[anneeCourante - 1] = interetsAnneeCourante;
                capitalRembourseParAnnee[anneeCourante - 1] = principalAnneeCourante;
                capitalRestantDuFinAnnee[anneeCourante - 1] = capitalRestant.longValueExact();
                assuranceParAnnee[anneeCourante - 1] = assuranceAnnuelleMontant.longValueExact();
                interetsAnneeCourante = 0;
                principalAnneeCourante = 0;
                anneeCourante++;
            }
        }

        return new TableauAmortissement(interetsParAnnee, capitalRembourseParAnnee, capitalRestantDuFinAnnee, assuranceParAnnee);
    }

    /**
     * Impute un résultat brut (foncier ou BIC) sur le solde de déficit reportable existant.
     * Si {@code plafondImputationRevenuGlobal} est non nul (réel foncier, D11), la part du déficit de
     * l'année excédant le solde imputable sur le revenu global est plafonnée à cette valeur ; sinon
     * (réel BIC, D21) tout déficit de l'année reste dans le solde reportable, jamais imputé au revenu global.
     */
    private static ResultatImputationDeficit imputerDeficit(
            BigDecimal resultatBrut, BigDecimal soldeDeficit, BigDecimal plafondImputationRevenuGlobal
    ) {
        if (resultatBrut.signum() >= 0) {
            BigDecimal imputationStock = resultatBrut.min(soldeDeficit);
            return new ResultatImputationDeficit(
                    resultatBrut.subtract(imputationStock), BigDecimal.ZERO, soldeDeficit.subtract(imputationStock));
        }
        BigDecimal deficitAnnee = resultatBrut.negate();
        BigDecimal imputationRevenuGlobal = plafondImputationRevenuGlobal != null
                ? deficitAnnee.min(plafondImputationRevenuGlobal)
                : BigDecimal.ZERO;
        BigDecimal nouveauSolde = soldeDeficit.add(deficitAnnee).subtract(imputationRevenuGlobal);
        return new ResultatImputationDeficit(BigDecimal.ZERO, imputationRevenuGlobal, nouveauSolde);
    }

    private record ResultatImputationDeficit(
            BigDecimal resultatImposable, BigDecimal deficitImputeRevenuGlobal, BigDecimal nouveauSolde) {
    }

    private static long versCentimes(BigDecimal valeur) {
        return valeur.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal versCentimesEnBigDecimal(BigDecimal valeur) {
        return valeur.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal arrondiPourcent(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private record TableauAmortissement(long[] interets, long[] capitalRembourse, long[] capitalRestantDu, long[] assurance) {
    }
}

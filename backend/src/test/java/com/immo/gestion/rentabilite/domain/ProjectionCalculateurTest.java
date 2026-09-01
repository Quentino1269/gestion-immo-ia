package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Moteur de calcul de la projection (§11 du slice). Les scénarios utilisent des montants
 * volontairement ronds pour permettre une vérification manuelle exacte, centime par centime.
 */
class ProjectionCalculateurTest {

    private static final ParametresAmortissement AMORTISSEMENT_DEFAUT =
            new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7);
    private static final ParametresChargesRecurrentes CHARGES_NULLES =
            new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L);

    private List<LigneRevenuSimule> ligne(long loyerMensuelEnCentimes) {
        return List.of(new LigneRevenuSimule(BienId.nouveau(), loyerMensuelEnCentimes, 0L));
    }

    private HypothesesEvolution hypotheses(String vacance, String indexLoyer, String indexCharges) {
        return new HypothesesEvolution(new BigDecimal(vacance), new BigDecimal(indexLoyer), new BigDecimal(indexCharges));
    }

    // --- A. Achat cash, MICRO_FONCIER, année unique ---

    @Test
    void cash_micro_foncier_calcule_rendements_attendus() {
        ParametresAcquisition acquisition = new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement cash = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.MICRO_FONCIER, 30, 1,
                acquisition, cash, AMORTISSEMENT_DEFAUT,
                ligne(100_000L), CHARGES_NULLES, hypotheses("0", "0", "0")
        );

        assertThat(projection).hasSize(1);
        LigneProjection l = projection.get(0);
        assertThat(l.loyerBrutAnnuelEnCentimes()).isEqualTo(1_200_000L);
        assertThat(l.chargesNonRecuperablesAnnuellesEnCentimes()).isZero();
        assertThat(l.interetsEmpruntAnnuelsEnCentimes()).isZero();
        assertThat(l.capitalRestantDuFinAnneeEnCentimes()).isZero();
        assertThat(l.amortissementBatiAnnuelEnCentimes()).isZero();
        assertThat(l.resultatImposableEnCentimes()).isEqualTo(840_000L); // abattement micro-foncier 30%
        assertThat(l.impotEstimeEnCentimes()).isEqualTo(396_480L); // 840 000 * (30% + 17,2%)
        assertThat(l.cashFlowApresFinancementApresImpotEnCentimes()).isEqualTo(803_520L);
        assertThat(l.rendementBrutPourcent()).isEqualByComparingTo("6.00");
        assertThat(l.rendementNetNetPourcent()).isEqualByComparingTo("4.02");
        assertThat(l.rendementSurFondsPropresPourcent()).isEqualByComparingTo("4.02");
    }

    // --- B. Régime réel foncier : déficit reporté puis absorbé (D11) ---

    @Test
    void reel_foncier_deficit_reporte_puis_absorbe_par_une_annee_positive() {
        ParametresAcquisition acquisition = new ParametresAcquisition(100_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement cash = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        ParametresChargesRecurrentes charges = new ParametresChargesRecurrentes(
                3_200_000L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L);

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.REEL_FONCIER, 30, 2,
                acquisition, cash, AMORTISSEMENT_DEFAUT,
                ligne(100_000L), charges, hypotheses("0", "300", "0")
        );

        LigneProjection annee1 = projection.get(0);
        assertThat(annee1.loyerBrutAnnuelEnCentimes()).isEqualTo(1_200_000L);
        assertThat(annee1.resultatImposableEnCentimes()).isZero(); // déficit intégralement absorbé
        assertThat(annee1.deficitReportableUtiliseEnCentimes()).isEqualTo(1_070_000L); // plafonné à 10 700 €
        assertThat(annee1.soldeDeficitFoncierReportableFinAnneeEnCentimes()).isEqualTo(930_000L);
        assertThat(annee1.impotEstimeEnCentimes()).isEqualTo(-321_000L); // gain fiscal : -1 070 000 * 30%

        LigneProjection annee2 = projection.get(1);
        assertThat(annee2.loyerBrutAnnuelEnCentimes()).isEqualTo(4_800_000L); // indexation x4
        assertThat(annee2.resultatImposableEnCentimes()).isEqualTo(670_000L); // 1 600 000 - 930 000 de stock absorbé
        assertThat(annee2.deficitReportableUtiliseEnCentimes()).isZero(); // pas de déficit cette année-là
        assertThat(annee2.soldeDeficitFoncierReportableFinAnneeEnCentimes()).isZero();
        assertThat(annee2.impotEstimeEnCentimes()).isEqualTo(316_240L);
        assertThat(annee2.cashFlowApresFinancementApresImpotEnCentimes()).isEqualTo(1_283_760L);
    }

    // --- D. Régime réel BIC : amortissement + déficit BIC reporté, jamais imputé sur le revenu global (D21) ---

    @Test
    void reel_bic_amortissement_et_deficit_reporte_sans_gain_fiscal_immediat() {
        ParametresAcquisition acquisition = new ParametresAcquisition(10_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement cash = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        ParametresAmortissement amortissement = new ParametresAmortissement(
                new BigDecimal("10"), new BigDecimal("10"), 20, 5);

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.REEL_BIC, 30, 2,
                acquisition, cash, amortissement,
                ligne(40_000L), CHARGES_NULLES, hypotheses("0", "300", "0")
        );

        LigneProjection annee1 = projection.get(0);
        assertThat(annee1.amortissementBatiAnnuelEnCentimes()).isEqualTo(400_000L); // 8 000 000 / 20
        assertThat(annee1.amortissementMobilierAnnuelEnCentimes()).isEqualTo(200_000L); // 1 000 000 / 5
        assertThat(annee1.resultatImposableEnCentimes()).isZero(); // déficit BIC (480 000 - 600 000)
        assertThat(annee1.soldeDeficitBicReportableFinAnneeEnCentimes()).isEqualTo(120_000L);
        // Contrairement au foncier, le déficit BIC de l'année ne génère aucun gain fiscal immédiat (D21).
        assertThat(annee1.impotEstimeEnCentimes()).isZero();

        LigneProjection annee2 = projection.get(1);
        assertThat(annee2.resultatImposableEnCentimes()).isEqualTo(1_200_000L); // 1 320 000 - 120 000 de stock absorbé
        assertThat(annee2.soldeDeficitBicReportableFinAnneeEnCentimes()).isZero();
        assertThat(annee2.impotEstimeEnCentimes()).isEqualTo(566_400L);
        assertThat(annee2.cashFlowApresFinancementApresImpotEnCentimes()).isEqualTo(1_353_600L);
    }

    // --- C. Tableau d'amortissement du prêt : invariants robustes à l'arrondi ---

    @Test
    void pret_amorti_integralement_a_l_echeance_puis_capital_reste_nul() {
        ParametresAcquisition acquisition = new ParametresAcquisition(1_500_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement credit = new ParametresFinancement(
                1_200_000L, new BigDecimal("6"), 2, new BigDecimal("1"));

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.MICRO_FONCIER, 0, 3,
                acquisition, credit, AMORTISSEMENT_DEFAUT,
                ligne(50_000L), CHARGES_NULLES, hypotheses("0", "0", "0")
        );

        LigneProjection annee1 = projection.get(0);
        LigneProjection annee2 = projection.get(1);
        LigneProjection annee3 = projection.get(2);

        // Le capital remboursé sur la durée du prêt doit exactement égaler le montant emprunté.
        assertThat(annee1.capitalRembourseAnnuelEnCentimes() + annee2.capitalRembourseAnnuelEnCentimes())
                .isEqualTo(1_200_000L);
        assertThat(annee1.capitalRestantDuFinAnneeEnCentimes()).isBetween(1L, 1_200_000L);
        assertThat(annee2.capitalRestantDuFinAnneeEnCentimes()).isZero(); // prêt soldé à l'échéance
        // Les intérêts diminuent d'année en année car le capital restant dû diminue.
        assertThat(annee1.interetsEmpruntAnnuelsEnCentimes()).isGreaterThan(annee2.interetsEmpruntAnnuelsEnCentimes());
        // Assurance constante sur le capital initial pendant la durée du prêt (D8).
        assertThat(annee1.assuranceEmprunteurAnnuelleEnCentimes()).isEqualTo(12_000L); // 1 200 000 * 1%
        assertThat(annee2.assuranceEmprunteurAnnuelleEnCentimes()).isEqualTo(12_000L);

        // Après l'échéance du prêt (année 3, horizon > durée), tout retombe à zéro.
        assertThat(annee3.interetsEmpruntAnnuelsEnCentimes()).isZero();
        assertThat(annee3.capitalRembourseAnnuelEnCentimes()).isZero();
        assertThat(annee3.assuranceEmprunteurAnnuelleEnCentimes()).isZero();
        assertThat(annee3.capitalRestantDuFinAnneeEnCentimes()).isZero();
    }

    // --- Rendement sur fonds propres non défini si apport nul ---

    @Test
    void rendement_sur_fonds_propres_null_si_apport_nul() {
        ParametresAcquisition acquisition = new ParametresAcquisition(1_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement credit = new ParametresFinancement(1_000_000L, BigDecimal.ZERO, 1, BigDecimal.ZERO);

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.MICRO_FONCIER, 0, 1,
                acquisition, credit, AMORTISSEMENT_DEFAUT,
                ligne(10_000L), CHARGES_NULLES, hypotheses("0", "0", "0")
        );

        assertThat(projection.get(0).rendementSurFondsPropresPourcent()).isNull();
    }

    // --- I-SIM-5 : horizonAnnees hors bornes refusé proprement (pas de NegativeArraySizeException) ---

    @Test
    void horizon_negatif_leve_illegal_argument_exception() {
        ParametresAcquisition acquisition = new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement cash = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);

        assertThatThrownBy(() -> ProjectionCalculateur.calculer(
                RegimeFiscal.MICRO_FONCIER, 30, -1,
                acquisition, cash, AMORTISSEMENT_DEFAUT,
                ligne(100_000L), CHARGES_NULLES, hypotheses("0", "0", "0")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horizonAnnees");
    }

    // --- Colocation : sommation des lignes de revenu (D4) ---

    @Test
    void revenus_de_plusieurs_lignes_sont_sommes() {
        ParametresAcquisition acquisition = new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L);
        ParametresFinancement cash = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        List<LigneRevenuSimule> deuxChambres = List.of(
                new LigneRevenuSimule(BienId.nouveau(), 60_000L, 0L),
                new LigneRevenuSimule(BienId.nouveau(), 40_000L, 0L)
        );

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                RegimeFiscal.MICRO_FONCIER, 0, 1,
                acquisition, cash, AMORTISSEMENT_DEFAUT,
                deuxChambres, CHARGES_NULLES, hypotheses("0", "0", "0")
        );

        assertThat(projection.get(0).loyerBrutAnnuelEnCentimes()).isEqualTo(1_200_000L);
    }
}

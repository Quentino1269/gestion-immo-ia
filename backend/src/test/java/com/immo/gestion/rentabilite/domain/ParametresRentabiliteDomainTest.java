package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariants des value objects du slice projection-rentabilite (§7, §9). L'aggregate
 * SimulationRentabilite est couvert séparément dans SimulationRentabiliteDomainTest.
 */
class ParametresRentabiliteDomainTest {

    // --- ParametresAcquisition (I-SIM-6) ---

    @Test
    void acquisition_valide_calcule_cout_total() {
        ParametresAcquisition a = new ParametresAcquisition(20_000_000L, 1_500_000L, 500_000L, 300_000L, 100_000L);
        assertThat(a.coutTotalEnCentimes()).isEqualTo(22_400_000L);
    }

    @Test
    void acquisition_prix_achat_nul_leve_exception() {
        assertThatThrownBy(() -> new ParametresAcquisition(0L, 0L, 0L, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prixAchatEnCentimes");
    }

    @Test
    void acquisition_frais_negatifs_leve_exception() {
        assertThatThrownBy(() -> new ParametresAcquisition(1_000_00L, -1L, 0L, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- ParametresFinancement (I-SIM-7 partiel, I-SIM-8, I-SIM-9) ---

    @Test
    void financement_cash_normalise_taux_et_duree_a_zero() {
        ParametresFinancement f = new ParametresFinancement(0L, new BigDecimal("3.5"), 20, BigDecimal.ZERO);
        assertThat(f.estCash()).isTrue();
        assertThat(f.tauxAnnuelPourcent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(f.dureeAnnees()).isZero();
    }

    @Test
    void financement_credit_conserve_les_valeurs_saisies() {
        ParametresFinancement f = new ParametresFinancement(10_000_000L, new BigDecimal("3.5"), 20, new BigDecimal("0.36"));
        assertThat(f.estCash()).isFalse();
        assertThat(f.tauxAnnuelPourcent()).isEqualByComparingTo("3.5");
        assertThat(f.dureeAnnees()).isEqualTo(20);
    }

    @Test
    void financement_montant_negatif_leve_exception() {
        assertThatThrownBy(() -> new ParametresFinancement(-1L, BigDecimal.ZERO, 0, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void financement_credit_sans_duree_leve_exception() {
        assertThatThrownBy(() -> new ParametresFinancement(10_000_00L, new BigDecimal("2"), 0, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeAnnees");
    }

    @Test
    void financement_credit_taux_null_leve_exception() {
        assertThatThrownBy(() -> new ParametresFinancement(10_000_00L, null, 10, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void financement_taux_assurance_negatif_leve_exception() {
        assertThatThrownBy(() -> new ParametresFinancement(0L, BigDecimal.ZERO, 0, new BigDecimal("-0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- ParametresAmortissement (I-SIM-10) ---

    @Test
    void amortissement_valide_calcule_quote_part_bati() {
        ParametresAmortissement a = new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7);
        assertThat(a.quotePartBatiPourcent()).isEqualByComparingTo("80");
    }

    @Test
    void amortissement_quote_parts_superieures_a_100_leve_exception() {
        assertThatThrownBy(() -> new ParametresAmortissement(new BigDecimal("60"), new BigDecimal("50"), 25, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void amortissement_quote_part_negative_leve_exception() {
        assertThatThrownBy(() -> new ParametresAmortissement(new BigDecimal("-1"), BigDecimal.ZERO, 25, 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void amortissement_duree_bati_nulle_leve_exception() {
        assertThatThrownBy(() -> new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 0, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeAmortissementBatiAnnees");
    }

    // --- ParametresChargesRecurrentes ---

    @Test
    void charges_recurrentes_valides_calcule_charges_fixes() {
        ParametresChargesRecurrentes c = new ParametresChargesRecurrentes(
                100_000L, 20_000L, 0L, new BigDecimal("8"), 50_000L, 0L, 0L);
        assertThat(c.chargesFixesEnCentimes()).isEqualTo(170_000L);
    }

    @Test
    void charges_recurrentes_montant_negatif_leve_exception() {
        assertThatThrownBy(() -> new ParametresChargesRecurrentes(-1L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void charges_recurrentes_frais_gestion_hors_bornes_leve_exception() {
        assertThatThrownBy(() -> new ParametresChargesRecurrentes(0L, 0L, 0L, new BigDecimal("101"), 0L, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- HypothesesEvolution (I-SIM-13, I-SIM-14) ---

    @Test
    void hypotheses_vacance_egale_100_leve_exception() {
        assertThatThrownBy(() -> new HypothesesEvolution(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hypotheses_indexation_loyer_negative_leve_exception() {
        assertThatThrownBy(() -> new HypothesesEvolution(BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- LigneRevenuSimule (I-SIM-12) ---

    @Test
    void ligne_revenu_loyer_negatif_leve_exception() {
        assertThatThrownBy(() -> new LigneRevenuSimule(BienId.nouveau(), -1L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ligne_revenu_bien_source_null_leve_exception() {
        assertThatThrownBy(() -> new LigneRevenuSimule(null, 0L, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    // --- RegimeFiscal (D6, I-SIM-3) ---

    @Test
    void regime_fiscal_compatibilite_avec_meuble() {
        assertThat(RegimeFiscal.MICRO_FONCIER.compatibleAvecMeuble(false)).isTrue();
        assertThat(RegimeFiscal.MICRO_FONCIER.compatibleAvecMeuble(true)).isFalse();
        assertThat(RegimeFiscal.REEL_FONCIER.compatibleAvecMeuble(false)).isTrue();
        assertThat(RegimeFiscal.MICRO_BIC.compatibleAvecMeuble(true)).isTrue();
        assertThat(RegimeFiscal.MICRO_BIC.compatibleAvecMeuble(false)).isFalse();
        assertThat(RegimeFiscal.REEL_BIC.compatibleAvecMeuble(true)).isTrue();
    }

    @Test
    void regime_fiscal_est_reel() {
        assertThat(RegimeFiscal.MICRO_FONCIER.estReel()).isFalse();
        assertThat(RegimeFiscal.REEL_FONCIER.estReel()).isTrue();
        assertThat(RegimeFiscal.MICRO_BIC.estReel()).isFalse();
        assertThat(RegimeFiscal.REEL_BIC.estReel()).isTrue();
    }
}

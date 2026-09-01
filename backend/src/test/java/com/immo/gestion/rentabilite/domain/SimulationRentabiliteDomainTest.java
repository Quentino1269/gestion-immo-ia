package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariants de l'aggregate SimulationRentabilite (I-SIM-4, I-SIM-5, I-SIM-7, I-SIM-17).
 * Cf. docs/slices/projection-rentabilite.md §9.
 */
class SimulationRentabiliteDomainTest {

    private static final Instant T = Instant.parse("2026-08-26T10:00:00Z");
    private static final ParametresAcquisition ACQUISITION = new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L);
    private static final ParametresFinancement CASH = new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO);
    private static final ParametresAmortissement AMORTISSEMENT =
            new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7);
    private static final ParametresChargesRecurrentes CHARGES =
            new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L);
    private static final HypothesesEvolution HYPOTHESES =
            new HypothesesEvolution(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private List<LigneRevenuSimule> uneLigneRevenu(BienId bienId) {
        return List.of(new LigneRevenuSimule(bienId, 100_000L, 0L));
    }

    private List<LigneProjection> projectionVide(int horizon) {
        List<LigneProjection> lignes = new ArrayList<>();
        for (int n = 1; n <= horizon; n++) {
            lignes.add(new LigneProjection(n, 1_200_000L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                    840_000L, 0L, 0L, 0L, 396_480L, 1_200_000L, 1_200_000L, 803_520L,
                    new BigDecimal("6.00"), new BigDecimal("6.00"), new BigDecimal("4.02"), new BigDecimal("4.02")));
        }
        return lignes;
    }

    private SimulationRentabilite simulation(String nomScenario, int tmi, int horizon,
                                              long coutTotal, long montantEmprunte) {
        BienId bienId = BienId.nouveau();
        return new SimulationRentabilite(
                SimulationRentabiliteId.nouveau(), bienId, UtilisateurId.nouveau(),
                nomScenario, RegimeFiscal.MICRO_FONCIER, tmi, horizon,
                new ParametresAcquisition(coutTotal, 0L, 0L, 0L, 0L),
                new ParametresFinancement(montantEmprunte,
                        montantEmprunte == 0 ? BigDecimal.ZERO : new BigDecimal("2"),
                        montantEmprunte == 0 ? 0 : 15, BigDecimal.ZERO),
                AMORTISSEMENT,
                uneLigneRevenu(bienId),
                CHARGES,
                HYPOTHESES,
                coutTotal,
                coutTotal - montantEmprunte,
                projectionVide(horizon),
                T
        );
    }

    // --- Nominal ---

    @Test
    void simulation_valide_se_construit() {
        SimulationRentabilite s = simulation("Achat cash", 30, 1, 20_000_000L, 0L);
        assertThat(s.apportPersonnelEnCentimes()).isEqualTo(20_000_000L);
        assertThat(s.coutTotalAcquisitionEnCentimes()).isEqualTo(20_000_000L);
        assertThat(s.projectionAnnuelle()).hasSize(1);
    }

    @Test
    void nom_scenario_est_strippe() {
        SimulationRentabilite s = simulation("  Achat cash  ", 30, 1, 20_000_000L, 0L);
        assertThat(s.nomScenario()).isEqualTo("Achat cash");
    }

    // --- I-SIM-4 ---

    @Test
    void tmi_invalide_leve_exception() {
        assertThatThrownBy(() -> simulation("Scenario", 25, 1, 20_000_000L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tmiFoyerPourcent");
    }

    // --- I-SIM-5 ---

    @Test
    void horizon_zero_leve_exception() {
        assertThatThrownBy(() -> simulation("Scenario", 30, 0, 20_000_000L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horizonAnnees");
    }

    @Test
    void horizon_superieur_a_40_leve_exception() {
        assertThatThrownBy(() -> simulation("Scenario", 30, 41, 20_000_000L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horizonAnnees");
    }

    // --- I-SIM-7 ---

    @Test
    void montant_emprunte_superieur_au_cout_total_leve_exception() {
        assertThatThrownBy(() -> simulation("Scenario", 30, 1, 10_000_000L, 20_000_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantEmprunteEnCentimes");
    }

    @Test
    void cout_total_incoherent_avec_acquisition_leve_exception() {
        BienId bienId = BienId.nouveau();
        assertThatThrownBy(() -> new SimulationRentabilite(
                SimulationRentabiliteId.nouveau(), bienId, UtilisateurId.nouveau(),
                "Scenario", RegimeFiscal.MICRO_FONCIER, 30, 1,
                ACQUISITION, CASH, AMORTISSEMENT, uneLigneRevenu(bienId), CHARGES, HYPOTHESES,
                999L, // incohérent avec ACQUISITION.coutTotalEnCentimes() = 20_000_000
                999L, projectionVide(1), T
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coutTotalAcquisitionEnCentimes");
    }

    // --- I-SIM-17 ---

    @Test
    void nom_scenario_vide_leve_exception() {
        assertThatThrownBy(() -> simulation("   ", 30, 1, 20_000_000L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nomScenario");
    }

    @Test
    void nom_scenario_trop_long_leve_exception() {
        String nomTropLong = "x".repeat(101);
        assertThatThrownBy(() -> simulation(nomTropLong, 30, 1, 20_000_000L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nomScenario");
    }

    // --- cohérence projection / horizon ---

    @Test
    void nombre_de_lignes_de_projection_different_de_l_horizon_leve_exception() {
        BienId bienId = BienId.nouveau();
        assertThatThrownBy(() -> new SimulationRentabilite(
                SimulationRentabiliteId.nouveau(), bienId, UtilisateurId.nouveau(),
                "Scenario", RegimeFiscal.MICRO_FONCIER, 30, 2,
                ACQUISITION, CASH, AMORTISSEMENT, uneLigneRevenu(bienId), CHARGES, HYPOTHESES,
                20_000_000L, 20_000_000L, projectionVide(1), T
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectionAnnuelle");
    }

    // --- I-SIM-11 (forme minimale) ---

    @Test
    void revenus_locatifs_simules_vide_leve_exception() {
        BienId bienId = BienId.nouveau();
        assertThatThrownBy(() -> new SimulationRentabilite(
                SimulationRentabiliteId.nouveau(), bienId, UtilisateurId.nouveau(),
                "Scenario", RegimeFiscal.MICRO_FONCIER, 30, 1,
                ACQUISITION, CASH, AMORTISSEMENT, List.of(), CHARGES, HYPOTHESES,
                20_000_000L, 20_000_000L, projectionVide(1), T
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusLocatifsSimules");
    }
}

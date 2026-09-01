package com.immo.gestion.rentabilite.adapter.web;

import com.immo.gestion.rentabilite.domain.LigneProjection;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SimulationRentabiliteResponse(
        UUID simulationId,
        UUID bienId,
        String nomScenario,
        RegimeFiscal regimeFiscal,
        int tmiFoyerPourcent,
        int horizonAnnees,
        AcquisitionResponse acquisition,
        FinancementResponse financement,
        AmortissementResponse amortissement,
        List<LigneRevenuResponse> revenusLocatifsSimules,
        ChargesRecurrentesResponse chargesRecurrentes,
        HypothesesEvolutionResponse hypothesesEvolution,
        long coutTotalAcquisitionEnCentimes,
        long apportPersonnelEnCentimes,
        List<LigneProjectionResponse> projectionAnnuelle,
        Instant simuleLe
) {

    public static SimulationRentabiliteResponse depuis(SimulationRentabilite s) {
        return new SimulationRentabiliteResponse(
                s.id().valeur(),
                s.bienId().valeur(),
                s.nomScenario(),
                s.regimeFiscal(),
                s.tmiFoyerPourcent(),
                s.horizonAnnees(),
                AcquisitionResponse.depuis(s.acquisition()),
                FinancementResponse.depuis(s.financement()),
                AmortissementResponse.depuis(s.amortissement()),
                s.revenusLocatifsSimules().stream().map(LigneRevenuResponse::depuis).toList(),
                ChargesRecurrentesResponse.depuis(s.chargesRecurrentes()),
                HypothesesEvolutionResponse.depuis(s.hypothesesEvolution()),
                s.coutTotalAcquisitionEnCentimes(),
                s.apportPersonnelEnCentimes(),
                s.projectionAnnuelle().stream().map(LigneProjectionResponse::depuis).toList(),
                s.simuleLe()
        );
    }

    public record AcquisitionResponse(
            long prixAchatEnCentimes,
            long fraisNotaireEnCentimes,
            long fraisAgenceEnCentimes,
            long travauxAlAcquisitionEnCentimes,
            long fraisDossierBancaireEnCentimes
    ) {
        public static AcquisitionResponse depuis(ParametresAcquisition p) {
            return new AcquisitionResponse(
                    p.prixAchatEnCentimes(), p.fraisNotaireEnCentimes(), p.fraisAgenceEnCentimes(),
                    p.travauxAlAcquisitionEnCentimes(), p.fraisDossierBancaireEnCentimes());
        }
    }

    public record FinancementResponse(
            long montantEmprunteEnCentimes,
            BigDecimal tauxAnnuelPourcent,
            int dureeAnnees,
            BigDecimal tauxAssuranceEmprunteurPourcent
    ) {
        public static FinancementResponse depuis(ParametresFinancement p) {
            return new FinancementResponse(
                    p.montantEmprunteEnCentimes(), p.tauxAnnuelPourcent(), p.dureeAnnees(), p.tauxAssuranceEmprunteurPourcent());
        }
    }

    public record AmortissementResponse(
            BigDecimal quotePartTerrainPourcent,
            BigDecimal quotePartMobilierPourcent,
            int dureeAmortissementBatiAnnees,
            int dureeAmortissementMobilierAnnees
    ) {
        public static AmortissementResponse depuis(ParametresAmortissement p) {
            return new AmortissementResponse(
                    p.quotePartTerrainPourcent(), p.quotePartMobilierPourcent(),
                    p.dureeAmortissementBatiAnnees(), p.dureeAmortissementMobilierAnnees());
        }
    }

    public record LigneRevenuResponse(
            UUID bienSourceId,
            long loyerSimuleMensuelEnCentimes,
            long chargesSimuleesMensuellesEnCentimes
    ) {
        public static LigneRevenuResponse depuis(LigneRevenuSimule l) {
            return new LigneRevenuResponse(
                    l.bienSourceId().valeur(), l.loyerSimuleMensuelEnCentimes(), l.chargesSimuleesMensuellesEnCentimes());
        }
    }

    public record ChargesRecurrentesResponse(
            long taxeFonciereEnCentimes,
            long assurancePnoEnCentimes,
            long assuranceLoyersImpayesEnCentimes,
            BigDecimal fraisGestionLocativePourcentLoyer,
            long provisionTravauxAnnuelleEnCentimes,
            long fraisComptabiliteAnnuelEnCentimes,
            long chargesCoproprieteNonRecuperablesEnCentimes
    ) {
        public static ChargesRecurrentesResponse depuis(ParametresChargesRecurrentes p) {
            return new ChargesRecurrentesResponse(
                    p.taxeFonciereEnCentimes(), p.assurancePnoEnCentimes(), p.assuranceLoyersImpayesEnCentimes(),
                    p.fraisGestionLocativePourcentLoyer(), p.provisionTravauxAnnuelleEnCentimes(),
                    p.fraisComptabiliteAnnuelEnCentimes(), p.chargesCoproprieteNonRecuperablesEnCentimes());
        }
    }

    public record HypothesesEvolutionResponse(
            BigDecimal tauxVacanceLocativePourcent,
            BigDecimal tauxIndexationLoyerPourcent,
            BigDecimal tauxIndexationChargesPourcent
    ) {
        public static HypothesesEvolutionResponse depuis(HypothesesEvolution h) {
            return new HypothesesEvolutionResponse(
                    h.tauxVacanceLocativePourcent(), h.tauxIndexationLoyerPourcent(), h.tauxIndexationChargesPourcent());
        }
    }

    public record LigneProjectionResponse(
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
        public static LigneProjectionResponse depuis(LigneProjection l) {
            return new LigneProjectionResponse(
                    l.annee(),
                    l.loyerBrutAnnuelEnCentimes(),
                    l.chargesNonRecuperablesAnnuellesEnCentimes(),
                    l.interetsEmpruntAnnuelsEnCentimes(),
                    l.capitalRembourseAnnuelEnCentimes(),
                    l.assuranceEmprunteurAnnuelleEnCentimes(),
                    l.capitalRestantDuFinAnneeEnCentimes(),
                    l.amortissementBatiAnnuelEnCentimes(),
                    l.amortissementMobilierAnnuelEnCentimes(),
                    l.resultatImposableEnCentimes(),
                    l.deficitReportableUtiliseEnCentimes(),
                    l.soldeDeficitFoncierReportableFinAnneeEnCentimes(),
                    l.soldeDeficitBicReportableFinAnneeEnCentimes(),
                    l.impotEstimeEnCentimes(),
                    l.cashFlowAvantFinancementAvantImpotEnCentimes(),
                    l.cashFlowApresFinancementAvantImpotEnCentimes(),
                    l.cashFlowApresFinancementApresImpotEnCentimes(),
                    l.rendementBrutPourcent(),
                    l.rendementNetPourcent(),
                    l.rendementNetNetPourcent(),
                    l.rendementSurFondsPropresPourcent()
            );
        }
    }
}

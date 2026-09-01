package com.immo.gestion.rentabilite.adapter.web;

import com.immo.gestion.rentabilite.domain.RegimeFiscal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LancerSimulationRentabiliteRequest(
        String nomScenario,
        RegimeFiscal regimeFiscal,
        int tmiFoyerPourcent,
        int horizonAnnees,
        AcquisitionRequest acquisition,
        FinancementRequest financement,
        AmortissementRequest amortissement,
        List<LigneRevenuRequest> revenusLocatifsSimules,
        ChargesRecurrentesRequest chargesRecurrentes,
        HypothesesEvolutionRequest hypothesesEvolution
) {

    public record AcquisitionRequest(
            long prixAchatEnCentimes,
            long fraisNotaireEnCentimes,
            long fraisAgenceEnCentimes,
            long travauxAlAcquisitionEnCentimes,
            long fraisDossierBancaireEnCentimes
    ) {}

    public record FinancementRequest(
            long montantEmprunteEnCentimes,
            BigDecimal tauxAnnuelPourcent,
            int dureeAnnees,
            BigDecimal tauxAssuranceEmprunteurPourcent
    ) {}

    public record AmortissementRequest(
            BigDecimal quotePartTerrainPourcent,
            BigDecimal quotePartMobilierPourcent,
            int dureeAmortissementBatiAnnees,
            int dureeAmortissementMobilierAnnees
    ) {}

    public record LigneRevenuRequest(
            UUID bienSourceId,
            long loyerSimuleMensuelEnCentimes,
            long chargesSimuleesMensuellesEnCentimes
    ) {}

    public record ChargesRecurrentesRequest(
            long taxeFonciereEnCentimes,
            long assurancePnoEnCentimes,
            long assuranceLoyersImpayesEnCentimes,
            BigDecimal fraisGestionLocativePourcentLoyer,
            long provisionTravauxAnnuelleEnCentimes,
            long fraisComptabiliteAnnuelEnCentimes,
            long chargesCoproprieteNonRecuperablesEnCentimes
    ) {}

    public record HypothesesEvolutionRequest(
            BigDecimal tauxVacanceLocativePourcent,
            BigDecimal tauxIndexationLoyerPourcent,
            BigDecimal tauxIndexationChargesPourcent
    ) {}
}

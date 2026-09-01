package com.immo.gestion.rentabilite.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** Read model — ligne résumée du comparateur de scénarios d'un bien. Cf. §10. */
public record LigneComparateur(
        SimulationRentabiliteId simulationId,
        String nomScenario,
        RegimeFiscal regimeFiscal,
        BigDecimal rendementBrutAnnee1Pourcent,
        BigDecimal rendementNetAnnee1Pourcent,
        BigDecimal rendementNetNetAnnee1Pourcent,
        long cashFlowMoyenApresImpotEnCentimes,
        Instant simuleLe
) {

    public static LigneComparateur depuis(SimulationRentabilite simulation) {
        LigneProjection annee1 = simulation.projectionAnnuelle().get(0);
        long cashFlowMoyen = Math.round(simulation.projectionAnnuelle().stream()
                .mapToLong(LigneProjection::cashFlowApresFinancementApresImpotEnCentimes)
                .average()
                .orElse(0d));
        return new LigneComparateur(
                simulation.id(),
                simulation.nomScenario(),
                simulation.regimeFiscal(),
                annee1.rendementBrutPourcent(),
                annee1.rendementNetPourcent(),
                annee1.rendementNetNetPourcent(),
                cashFlowMoyen,
                simulation.simuleLe()
        );
    }
}

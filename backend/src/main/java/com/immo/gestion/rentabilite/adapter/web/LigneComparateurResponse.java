package com.immo.gestion.rentabilite.adapter.web;

import com.immo.gestion.rentabilite.domain.LigneComparateur;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LigneComparateurResponse(
        UUID simulationId,
        String nomScenario,
        RegimeFiscal regimeFiscal,
        BigDecimal rendementBrutAnnee1Pourcent,
        BigDecimal rendementNetAnnee1Pourcent,
        BigDecimal rendementNetNetAnnee1Pourcent,
        long cashFlowMoyenApresImpotEnCentimes,
        Instant simuleLe
) {

    public static LigneComparateurResponse depuis(LigneComparateur l) {
        return new LigneComparateurResponse(
                l.simulationId().valeur(),
                l.nomScenario(),
                l.regimeFiscal(),
                l.rendementBrutAnnee1Pourcent(),
                l.rendementNetAnnee1Pourcent(),
                l.rendementNetNetAnnee1Pourcent(),
                l.cashFlowMoyenApresImpotEnCentimes(),
                l.simuleLe()
        );
    }
}

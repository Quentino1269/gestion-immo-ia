package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.List;

/**
 * Événement émis quand une simulation existante est modifiée. N'écrase jamais l'historique
 * (append-only, D2 revisité) : ce fait s'ajoute au flux de la simulation, qui garde son identité
 * (même {@code simulationId}, même {@code nomScenario} sauf renommage explicite) — la simulation
 * reste consultable dans ses versions antérieures via l'historique du flux.
 */
public record SimulationRentabiliteModifiee(
        SimulationRentabiliteId simulationId,
        String nomScenario,
        RegimeFiscal regimeFiscal,
        int tmiFoyerPourcent,
        int horizonAnnees,
        ParametresAcquisition acquisition,
        ParametresFinancement financement,
        ParametresAmortissement amortissement,
        List<LigneRevenuSimule> revenusLocatifsSimules,
        ParametresChargesRecurrentes chargesRecurrentes,
        HypothesesEvolution hypothesesEvolution,
        long coutTotalAcquisitionEnCentimes,
        long apportPersonnelEnCentimes,
        List<LigneProjection> projectionAnnuelle,
        Instant survenuLe
) implements DomainEvent {

    public static SimulationRentabiliteModifiee depuis(SimulationRentabilite simulation) {
        return new SimulationRentabiliteModifiee(
                simulation.id(),
                simulation.nomScenario(),
                simulation.regimeFiscal(),
                simulation.tmiFoyerPourcent(),
                simulation.horizonAnnees(),
                simulation.acquisition(),
                simulation.financement(),
                simulation.amortissement(),
                simulation.revenusLocatifsSimules(),
                simulation.chargesRecurrentes(),
                simulation.hypothesesEvolution(),
                simulation.coutTotalAcquisitionEnCentimes(),
                simulation.apportPersonnelEnCentimes(),
                simulation.projectionAnnuelle(),
                simulation.simuleLe()
        );
    }
}

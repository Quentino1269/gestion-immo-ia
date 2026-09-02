package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;
import java.util.List;

/**
 * Événement émis quand une simulation de rentabilité est calculée. Fait immuable (D2) :
 * reporte l'intégralité des paramètres saisis et du résultat calculé.
 * Cf. docs/slices/projection-rentabilite.md §8.
 */
public record RentabiliteSimulee(
        SimulationRentabiliteId simulationId,
        BienId bienId,
        UtilisateurId utilisateurId,
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

    public static RentabiliteSimulee depuis(SimulationRentabilite simulation) {
        return new RentabiliteSimulee(
                simulation.id(),
                simulation.bienId(),
                simulation.utilisateurId(),
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

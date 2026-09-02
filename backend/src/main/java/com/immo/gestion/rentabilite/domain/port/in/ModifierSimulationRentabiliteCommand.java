package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;

/** {@code bienId} n'y figure pas : une modification ne change jamais le bien visé, il est lu depuis
 * la simulation existante. */
public record ModifierSimulationRentabiliteCommand(
        SimulationRentabiliteId simulationId,
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
        HypothesesEvolution hypothesesEvolution
) {
}

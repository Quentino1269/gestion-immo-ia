package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

public record SupprimerSimulationCommand(
        SimulationRentabiliteId simulationId,
        UtilisateurId demandeurId
) {
}

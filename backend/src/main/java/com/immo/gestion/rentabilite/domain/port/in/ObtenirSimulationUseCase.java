package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

public interface ObtenirSimulationUseCase {

    SimulationRentabilite obtenir(SimulationRentabiliteId id, UtilisateurId demandeurId);
}

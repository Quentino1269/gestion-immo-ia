package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;

public interface LancerSimulationRentabiliteUseCase {

    SimulationRentabilite lancer(LancerSimulationRentabiliteCommand commande);
}

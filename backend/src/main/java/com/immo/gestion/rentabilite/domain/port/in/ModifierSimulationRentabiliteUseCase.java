package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;

public interface ModifierSimulationRentabiliteUseCase {

    SimulationRentabilite modifier(ModifierSimulationRentabiliteCommand commande);
}

package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;

public class SimulationSupprimeeException extends RuntimeException {

    public SimulationSupprimeeException(SimulationRentabiliteId id) {
        super("Simulation déjà supprimée : " + id.valeur());
    }
}

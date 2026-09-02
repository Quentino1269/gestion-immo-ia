package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;

public class SimulationNonTrouveeException extends RuntimeException {

    public SimulationNonTrouveeException(SimulationRentabiliteId id) {
        super("Simulation introuvable : " + id.valeur());
    }
}

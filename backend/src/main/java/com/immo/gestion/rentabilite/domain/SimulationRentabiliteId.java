package com.immo.gestion.rentabilite.domain;

import java.util.Objects;
import java.util.UUID;

public record SimulationRentabiliteId(UUID valeur) {

    public SimulationRentabiliteId {
        Objects.requireNonNull(valeur, "simulationId requis");
    }

    public static SimulationRentabiliteId nouveau() {
        return new SimulationRentabiliteId(UUID.randomUUID());
    }
}

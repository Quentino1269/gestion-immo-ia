package com.immo.gestion.rentabilite.domain.port.out;

import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.shared.domain.DomainEvent;

import java.util.List;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate. Cf. MISSION.md §5.
 * Les lectures passent par {@link SimulationRentabiliteQueryRepository}.
 */
public interface SimulationRentabiliteRepository {

    void enregistrer(SimulationRentabiliteId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);
}

package com.immo.gestion.rentabilite.domain.port.out;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;

import java.util.List;
import java.util.Optional;

/**
 * Lecture, adossée à la projection {@code simulations_rentabilite} (read model). Cf. MISSION.md §5.
 */
public interface SimulationRentabiliteQueryRepository {

    Optional<SimulationRentabilite> chargerParId(SimulationRentabiliteId id);

    List<SimulationRentabilite> chargerParBien(BienId bienId);
}

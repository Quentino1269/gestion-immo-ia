package com.immo.gestion.rentabilite.domain.port.out;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;

import java.util.List;
import java.util.Optional;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate et rejeu de l'aggregate qu'on
 * s'apprête à muter. Cf. MISSION.md §5. Les autres lectures passent par
 * {@link SimulationRentabiliteQueryRepository}.
 */
public interface SimulationRentabiliteRepository {

    void enregistrer(SimulationRentabiliteId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);

    Optional<EtatCharge<SimulationRentabilite>> chargerParId(SimulationRentabiliteId id);

    /** Historique des versions du flux, de la plus ancienne à la plus récente. Vide si le flux n'existe pas. */
    List<SimulationRentabilite> chargerHistorique(SimulationRentabiliteId id);
}

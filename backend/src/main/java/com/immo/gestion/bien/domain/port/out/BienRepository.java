package com.immo.gestion.bien.domain.port.out;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.shared.domain.DomainEvent;

import java.util.List;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate. Cf. MISSION.md §5.
 * Les lectures passent par {@link BienQueryRepository}.
 */
public interface BienRepository {

    void enregistrer(BienId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);
}

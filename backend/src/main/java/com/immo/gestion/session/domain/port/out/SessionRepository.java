package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;

import java.util.List;
import java.util.Optional;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate et rejeu de l'aggregate qu'on
 * s'apprête à muter. Cf. MISSION.md §5. Les autres lectures passent par {@link SessionQueryRepository}.
 */
public interface SessionRepository {

    void enregistrer(SessionId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);

    Optional<EtatCharge<Session>> chargerParId(SessionId id);
}

package com.immo.gestion.bien.domain.port.out;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;

import java.util.List;
import java.util.Optional;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate, et rechargement par rejeu
 * avant modification. Cf. MISSION.md §5. Les lectures pures passent par {@link BienQueryRepository}.
 */
public interface BienRepository {

    void enregistrer(BienId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);

    Optional<EtatCharge<Bien>> chargerParId(BienId id);
}

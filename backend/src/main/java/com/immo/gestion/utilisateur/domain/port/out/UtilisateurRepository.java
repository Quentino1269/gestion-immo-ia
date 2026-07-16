package com.immo.gestion.utilisateur.domain.port.out;

import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;
import java.util.Optional;

/**
 * Port d'écriture (event-sourcé) : append au flux de l'aggregate et rejeu de l'aggregate qu'on
 * s'apprête à muter. Cf. MISSION.md §5. Les autres lectures passent par {@link UtilisateurQueryRepository}.
 */
public interface UtilisateurRepository {

    void enregistrer(UtilisateurId id, long expectedVersion, List<DomainEvent> nouveauxEvenements);

    Optional<EtatCharge<Utilisateur>> chargerParId(UtilisateurId id);
}

package com.immo.gestion.bien.domain.port.out;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;
import java.util.Optional;

/**
 * Port de lecture, adossé à la projection {@code biens} (read model). Cf. MISSION.md §5.
 */
public interface BienQueryRepository {

    Optional<Bien> chargerParId(BienId id);

    List<Bien> chargerParProprietaire(UtilisateurId proprietaireId);

    List<Bien> chargerChambresParParent(BienId parentId);
}

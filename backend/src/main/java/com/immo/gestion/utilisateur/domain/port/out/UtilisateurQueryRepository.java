package com.immo.gestion.utilisateur.domain.port.out;

import com.immo.gestion.shared.Email;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.Optional;

/**
 * Port de lecture, adossé à la projection {@code utilisateurs} (read model). Cf. MISSION.md §5.
 */
public interface UtilisateurQueryRepository {

    boolean existeParEmail(Email email);

    Optional<Utilisateur> chargerParId(UtilisateurId id);
}

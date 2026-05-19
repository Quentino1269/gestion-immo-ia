package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.shared.Email;

import java.util.Optional;

/**
 * Port d'accès au read model {@code RepertoireAuthentification} (slice
 * creation-utilisateur §10). Implémenté côté session par un adapter qui
 * consulte la persistance utilisateur, sans coupler les deux domaines.
 */
public interface RepertoireAuthentification {

    Optional<InfosAuthentification> chercherParEmail(Email email);
}

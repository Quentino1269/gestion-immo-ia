package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.TokenSessionHash;

import java.util.Optional;

/**
 * Port de lecture, adossé à la projection {@code sessions} (read model). Cf. MISSION.md §5.
 */
public interface SessionQueryRepository {

    Optional<Session> chargerParTokenHash(TokenSessionHash hash);
}

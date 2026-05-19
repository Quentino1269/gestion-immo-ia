package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TokenSessionHash;

import java.util.Optional;

public interface SessionRepository {

    void enregistrer(Session session);

    Optional<Session> chargerParId(SessionId id);

    Optional<Session> chargerParTokenHash(TokenSessionHash hash);
}

package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TokenSessionHash;
import com.immo.gestion.session.domain.port.out.SessionQueryRepository;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Lecture adossée à la projection {@code sessions}, maintenue à jour par {@link SessionProjectionListener}.
 */
@Repository
public class SessionQueryRepositoryAdapter implements SessionQueryRepository {

    private final SessionJpaRepository jpa;

    public SessionQueryRepositoryAdapter(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Session> chargerParTokenHash(TokenSessionHash hash) {
        return jpa.findByTokenHash(hash.hex()).map(this::versDomaine);
    }

    private Session versDomaine(SessionEntity e) {
        return new Session(
                new SessionId(e.getId()),
                new UtilisateurId(e.getUtilisateurId()),
                new TokenSessionHash(e.getTokenHash()),
                e.getOuverteLe(),
                e.getExpireA(),
                e.getEtat(),
                e.getMotifFermeture(),
                e.getFermeeLe(),
                e.getUserAgent(),
                e.getIpSource()
        );
    }
}

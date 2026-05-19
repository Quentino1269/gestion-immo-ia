package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TokenSessionHash;
import com.immo.gestion.session.domain.port.out.SessionRepository;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpa;

    public SessionRepositoryAdapter(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void enregistrer(Session session) {
        jpa.save(new SessionEntity(
                session.id().valeur(),
                session.utilisateurId().valeur(),
                session.tokenHash().hex(),
                session.ouverteLe(),
                session.expireA(),
                session.etat(),
                session.motifFermeture(),
                session.fermeeLe(),
                session.userAgentOpt().orElse(null),
                session.ipSourceOpt().orElse(null)
        ));
    }

    @Override
    public Optional<Session> chargerParId(SessionId id) {
        return jpa.findById(id.valeur()).map(this::versDomaine);
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

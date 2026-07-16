package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.port.out.SessionRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SessionRepositoryAdapter implements SessionRepository {

    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;

    public SessionRepositoryAdapter(EventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void enregistrer(SessionId id, long expectedVersion, List<DomainEvent> nouveauxEvenements) {
        eventStore.append(id.valeur(), "Session", expectedVersion, nouveauxEvenements);
        nouveauxEvenements.forEach(eventPublisher::publishEvent);
    }

    @Override
    public Optional<EtatCharge<Session>> chargerParId(SessionId id) {
        List<DomainEvent> evenements = eventStore.charger(id.valeur());
        if (evenements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EtatCharge<>(Session.reconstruire(evenements), evenements.size()));
    }
}

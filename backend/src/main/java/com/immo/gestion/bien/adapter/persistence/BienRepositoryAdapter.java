package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BienRepositoryAdapter implements BienRepository {

    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;

    public BienRepositoryAdapter(EventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void enregistrer(BienId id, long expectedVersion, List<DomainEvent> nouveauxEvenements) {
        eventStore.append(id.valeur(), "Bien", expectedVersion, nouveauxEvenements);
        nouveauxEvenements.forEach(eventPublisher::publishEvent);
    }
}

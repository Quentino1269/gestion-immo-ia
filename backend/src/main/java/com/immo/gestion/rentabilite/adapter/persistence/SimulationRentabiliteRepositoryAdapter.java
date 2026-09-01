package com.immo.gestion.rentabilite.adapter.persistence;

import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SimulationRentabiliteRepositoryAdapter implements SimulationRentabiliteRepository {

    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;

    public SimulationRentabiliteRepositoryAdapter(EventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void enregistrer(SimulationRentabiliteId id, long expectedVersion, List<DomainEvent> nouveauxEvenements) {
        eventStore.append(id.valeur(), "SimulationRentabilite", expectedVersion, nouveauxEvenements);
        nouveauxEvenements.forEach(eventPublisher::publishEvent);
    }
}

package com.immo.gestion.rentabilite.adapter.persistence;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<EtatCharge<SimulationRentabilite>> chargerParId(SimulationRentabiliteId id) {
        List<DomainEvent> evenements = eventStore.charger(id.valeur());
        if (evenements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EtatCharge<>(SimulationRentabilite.reconstruire(evenements), evenements.size()));
    }

    @Override
    public List<SimulationRentabilite> chargerHistorique(SimulationRentabiliteId id) {
        List<DomainEvent> evenements = eventStore.charger(id.valeur());
        if (evenements.isEmpty()) {
            return List.of();
        }
        return SimulationRentabilite.reconstruireHistorique(evenements);
    }
}

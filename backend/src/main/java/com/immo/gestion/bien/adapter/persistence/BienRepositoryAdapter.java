package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<EtatCharge<Bien>> chargerParId(BienId id) {
        List<DomainEvent> evenements = eventStore.charger(id.valeur());
        if (evenements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EtatCharge<>(Bien.reconstruire(evenements), evenements.size()));
    }
}

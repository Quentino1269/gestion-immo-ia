package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.shared.domain.port.out.EventStore;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UtilisateurRepositoryAdapter implements UtilisateurRepository {

    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;

    public UtilisateurRepositoryAdapter(EventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void enregistrer(UtilisateurId id, long expectedVersion, List<DomainEvent> nouveauxEvenements) {
        eventStore.append(id.valeur(), "Utilisateur", expectedVersion, nouveauxEvenements);
        nouveauxEvenements.forEach(eventPublisher::publishEvent);
    }

    @Override
    public Optional<EtatCharge<Utilisateur>> chargerParId(UtilisateurId id) {
        List<DomainEvent> evenements = eventStore.charger(id.valeur());
        if (evenements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EtatCharge<>(Utilisateur.reconstruire(evenements), evenements.size()));
    }
}

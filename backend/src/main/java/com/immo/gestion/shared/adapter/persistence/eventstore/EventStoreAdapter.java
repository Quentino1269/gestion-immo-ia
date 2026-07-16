package com.immo.gestion.shared.adapter.persistence.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.port.out.ConflitDeVersionException;
import com.immo.gestion.shared.domain.port.out.EventStore;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class EventStoreAdapter implements EventStore {

    private final EventStoreJpaRepository jpa;
    private final DomainEventTypeRegistry registry;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EventStoreAdapter(EventStoreJpaRepository jpa, DomainEventTypeRegistry registry,
                              ObjectMapper objectMapper, Clock clock) {
        this.jpa = jpa;
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void append(UUID streamId, String streamType, long expectedVersion, List<DomainEvent> nouveauxEvenements) {
        if (nouveauxEvenements.isEmpty()) {
            return;
        }
        Instant enregistreLe = Instant.now(clock);
        List<EventStoreEntry> lignes = new ArrayList<>(nouveauxEvenements.size());
        long version = expectedVersion;
        for (DomainEvent evenement : nouveauxEvenements) {
            version++;
            lignes.add(new EventStoreEntry(
                    streamId,
                    streamType,
                    version,
                    registry.clefPour(evenement),
                    ecrire(evenement),
                    evenement.survenuLe(),
                    enregistreLe
            ));
        }
        try {
            jpa.saveAll(lignes);
            jpa.flush();
        } catch (DataIntegrityViolationException e) {
            // Ne traduit que la violation de la contrainte OCC ; toute autre violation d'intégrité
            // (bug de sérialisation, colonne trop longue, etc.) doit remonter telle quelle plutôt
            // que d'être prise pour un conflit de version et retentée indéfiniment par l'appelant.
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new ConflitDeVersionException(streamId, expectedVersion, e);
            }
            throw e;
        }
    }

    @Override
    public List<DomainEvent> charger(UUID streamId) {
        return jpa.findByStreamIdOrderByVersionAsc(streamId).stream()
                .map(this::versDomaine)
                .toList();
    }

    private String ecrire(DomainEvent evenement) {
        try {
            return objectMapper.writeValueAsString(evenement);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation impossible de l'événement " + evenement.getClass(), e);
        }
    }

    private DomainEvent versDomaine(EventStoreEntry ligne) {
        Class<? extends DomainEvent> type = registry.resoudre(ligne.getEventType());
        try {
            return objectMapper.readValue(ligne.getPayload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Désérialisation impossible de l'événement " + ligne.getEventType(), e);
        }
    }
}

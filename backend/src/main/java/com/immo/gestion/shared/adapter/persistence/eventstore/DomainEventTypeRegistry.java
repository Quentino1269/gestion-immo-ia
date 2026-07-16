package com.immo.gestion.shared.adapter.persistence.eventstore;

import com.immo.gestion.shared.domain.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre explicite {@code event_type -> classe Java}, alimenté au démarrage par un
 * "registrar" par bounded context (ex. {@code BienEventTypesRegistrar}). Choisi plutôt que le
 * nom qualifié Java pour découpler le contenu de l'event store d'un futur refactor de package.
 */
@Component
public class DomainEventTypeRegistry {

    private final Map<String, Class<? extends DomainEvent>> typesParClef = new ConcurrentHashMap<>();

    public void enregistrer(Class<? extends DomainEvent> type) {
        Class<? extends DomainEvent> existant = typesParClef.putIfAbsent(type.getSimpleName(), type);
        if (existant != null && existant != type) {
            throw new IllegalStateException(
                    "Collision dans le registre d'événements : " + type.getSimpleName()
                            + " désigne à la fois " + existant.getName() + " et " + type.getName());
        }
    }

    /**
     * Valide l'enregistrement dès l'écriture plutôt que de laisser un type non enregistré
     * échouer seulement au rejeu (potentiellement en production, sur une donnée déjà persistée).
     */
    public String clefPour(DomainEvent evenement) {
        String clef = evenement.getClass().getSimpleName();
        if (!typesParClef.containsKey(clef)) {
            throw new EventTypeInconnuException(clef);
        }
        return clef;
    }

    public Class<? extends DomainEvent> resoudre(String clef) {
        Class<? extends DomainEvent> type = typesParClef.get(clef);
        if (type == null) {
            throw new EventTypeInconnuException(clef);
        }
        return type;
    }
}

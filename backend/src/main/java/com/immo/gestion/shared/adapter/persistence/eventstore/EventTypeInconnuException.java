package com.immo.gestion.shared.adapter.persistence.eventstore;

public class EventTypeInconnuException extends RuntimeException {

    public EventTypeInconnuException(String eventType) {
        super("Type d'événement inconnu dans le registre : " + eventType);
    }
}

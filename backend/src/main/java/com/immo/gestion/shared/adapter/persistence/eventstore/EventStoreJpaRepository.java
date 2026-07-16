package com.immo.gestion.shared.adapter.persistence.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventStoreJpaRepository extends JpaRepository<EventStoreEntry, Long> {

    List<EventStoreEntry> findByStreamIdOrderByVersionAsc(UUID streamId);
}

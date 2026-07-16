package com.immo.gestion.shared.adapter.persistence.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.port.out.ConflitDeVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventStoreAdapterTest {

    private static final Instant T = Instant.parse("2026-07-15T10:00:00Z");

    private record EvenementTest(String valeur, Instant survenuLe) implements DomainEvent {
    }

    private EventStoreJpaRepository jpa;
    private EventStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        jpa = mock(EventStoreJpaRepository.class);
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();
        registry.enregistrer(EvenementTest.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Clock clock = Clock.fixed(T, ZoneOffset.UTC);
        adapter = new EventStoreAdapter(jpa, registry, objectMapper, clock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void append_assigne_les_versions_a_partir_de_la_version_attendue() {
        UUID streamId = UUID.randomUUID();

        adapter.append(streamId, "Test", 2L, List.of(new EvenementTest("x", T), new EvenementTest("y", T)));

        ArgumentCaptor<List<EventStoreEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(jpa).saveAll(captor.capture());
        List<EventStoreEntry> lignes = captor.getValue();
        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).getVersion()).isEqualTo(3L);
        assertThat(lignes.get(1).getVersion()).isEqualTo(4L);
        assertThat(lignes.get(0).getStreamId()).isEqualTo(streamId);
        assertThat(lignes.get(0).getEventType()).isEqualTo("EvenementTest");
        assertThat(lignes.get(0).getPayload()).contains("\"valeur\":\"x\"");
        verify(jpa).flush();
    }

    @Test
    void append_ignore_une_liste_vide() {
        adapter.append(UUID.randomUUID(), "Test", 0L, List.of());

        verifyNoInteractions(jpa);
    }

    @Test
    void append_traduit_une_violation_de_contrainte_en_conflit_de_version() {
        when(jpa.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("dup"));
        UUID streamId = UUID.randomUUID();

        assertThatThrownBy(() -> adapter.append(streamId, "Test", 0L, List.of(new EvenementTest("x", T))))
                .isInstanceOf(ConflitDeVersionException.class);
    }

    @Test
    void charger_deserialise_les_evenements_dans_l_ordre() {
        UUID streamId = UUID.randomUUID();
        EventStoreEntry ligne = new EventStoreEntry(
                streamId, "Test", 1L, "EvenementTest",
                "{\"valeur\":\"x\",\"survenuLe\":\"2026-07-15T10:00:00Z\"}", T, T
        );
        when(jpa.findByStreamIdOrderByVersionAsc(streamId)).thenReturn(List.of(ligne));

        List<DomainEvent> evenements = adapter.charger(streamId);

        assertThat(evenements).containsExactly(new EvenementTest("x", T));
    }
}

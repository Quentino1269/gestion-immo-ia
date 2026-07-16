package com.immo.gestion.shared.adapter.persistence.eventstore;

import com.immo.gestion.shared.domain.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEventTypeRegistryTest {

    private record EvenementTest(Instant survenuLe) implements DomainEvent {
    }

    @Test
    void enregistrer_puis_resoudre_retourne_la_meme_classe() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();
        registry.enregistrer(EvenementTest.class);

        assertThat(registry.resoudre("EvenementTest")).isEqualTo(EvenementTest.class);
    }

    @Test
    void clefPour_retourne_le_nom_simple_de_la_classe_si_enregistree() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();
        registry.enregistrer(EvenementTest.class);

        assertThat(registry.clefPour(new EvenementTest(Instant.now()))).isEqualTo("EvenementTest");
    }

    @Test
    void clefPour_type_non_enregistre_leve_une_exception() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();

        assertThatThrownBy(() -> registry.clefPour(new EvenementTest(Instant.now())))
                .isInstanceOf(EventTypeInconnuException.class);
    }

    @Test
    void resoudre_type_non_enregistre_leve_une_exception() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();

        assertThatThrownBy(() -> registry.resoudre("Inconnu"))
                .isInstanceOf(EventTypeInconnuException.class);
    }

    @Test
    void enregistrer_deux_fois_la_meme_classe_est_sans_effet() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();
        registry.enregistrer(EvenementTest.class);
        registry.enregistrer(EvenementTest.class);

        assertThat(registry.resoudre("EvenementTest")).isEqualTo(EvenementTest.class);
    }

    @Test
    void enregistrer_une_collision_de_nom_leve_une_exception() {
        DomainEventTypeRegistry registry = new DomainEventTypeRegistry();
        registry.enregistrer(EvenementTest.class);

        // Même simple name "EvenementTest", classe différente (package eventstore.collision).
        assertThatThrownBy(() -> registry.enregistrer(
                com.immo.gestion.shared.adapter.persistence.eventstore.collision.EvenementTest.class))
                .isInstanceOf(IllegalStateException.class);
    }
}

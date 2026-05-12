package com.immo.gestion.utilisateur.test;

import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Capteur des événements {@link UtilisateurInscrit} publiés en test.
 * Bean component-scanné par {@code @SpringBootTest} (présent dans
 * {@code src/test/java}, ne pollue pas la prod).
 */
@Component
public class CapteurEventsUtilisateurInscrit {

    private final List<UtilisateurInscrit> events = new ArrayList<>();

    @EventListener
    void capter(UtilisateurInscrit event) {
        events.add(event);
    }

    public List<UtilisateurInscrit> events() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}

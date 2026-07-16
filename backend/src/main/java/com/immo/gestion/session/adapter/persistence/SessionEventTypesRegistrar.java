package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.UtilisateurConnecte;
import com.immo.gestion.session.domain.UtilisateurDeconnecte;
import com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry;
import org.springframework.stereotype.Component;

@Component
public class SessionEventTypesRegistrar {

    public SessionEventTypesRegistrar(DomainEventTypeRegistry registry) {
        registry.enregistrer(UtilisateurConnecte.class);
        registry.enregistrer(UtilisateurDeconnecte.class);
    }
}

package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry;
import org.springframework.stereotype.Component;

@Component
public class BienEventTypesRegistrar {

    public BienEventTypesRegistrar(DomainEventTypeRegistry registry) {
        registry.enregistrer(BienAjouteAuPortefeuille.class);
    }
}

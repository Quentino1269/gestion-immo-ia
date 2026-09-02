package com.immo.gestion.rentabilite.adapter.persistence;

import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteModifiee;
import com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry;
import org.springframework.stereotype.Component;

@Component
public class SimulationRentabiliteEventTypesRegistrar {

    public SimulationRentabiliteEventTypesRegistrar(DomainEventTypeRegistry registry) {
        registry.enregistrer(RentabiliteSimulee.class);
        registry.enregistrer(SimulationRentabiliteModifiee.class);
    }
}

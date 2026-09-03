package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.bien.domain.ChargesRevisees;
import com.immo.gestion.bien.domain.DisponibiliteRevisee;
import com.immo.gestion.bien.domain.LibelleChambreRenomme;
import com.immo.gestion.bien.domain.LogementDevenuNu;
import com.immo.gestion.bien.domain.LoyerRevise;
import com.immo.gestion.bien.domain.MeubleEntreDansLeLogement;
import com.immo.gestion.bien.domain.NombrePiecesRevise;
import com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry;
import org.springframework.stereotype.Component;

@Component
public class BienEventTypesRegistrar {

    public BienEventTypesRegistrar(DomainEventTypeRegistry registry) {
        registry.enregistrer(BienAjouteAuPortefeuille.class);
        registry.enregistrer(LoyerRevise.class);
        registry.enregistrer(ChargesRevisees.class);
        registry.enregistrer(MeubleEntreDansLeLogement.class);
        registry.enregistrer(LogementDevenuNu.class);
        registry.enregistrer(DisponibiliteRevisee.class);
        registry.enregistrer(LibelleChambreRenomme.class);
        registry.enregistrer(NombrePiecesRevise.class);
    }
}

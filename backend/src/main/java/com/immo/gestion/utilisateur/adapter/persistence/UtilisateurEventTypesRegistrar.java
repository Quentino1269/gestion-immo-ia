package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.adapter.persistence.eventstore.DomainEventTypeRegistry;
import com.immo.gestion.utilisateur.domain.AdresseDomicileRenseignee;
import com.immo.gestion.utilisateur.domain.CiviliteRenseignee;
import com.immo.gestion.utilisateur.domain.DonneesNaissanceRenseignees;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateurComplete;
import com.immo.gestion.utilisateur.domain.TelephoneRenseigne;
import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurEventTypesRegistrar {

    public UtilisateurEventTypesRegistrar(DomainEventTypeRegistry registry) {
        registry.enregistrer(UtilisateurInscrit.class);
        registry.enregistrer(CiviliteRenseignee.class);
        registry.enregistrer(DonneesNaissanceRenseignees.class);
        registry.enregistrer(AdresseDomicileRenseignee.class);
        registry.enregistrer(TelephoneRenseigne.class);
        registry.enregistrer(ProfilUtilisateurComplete.class);
    }
}

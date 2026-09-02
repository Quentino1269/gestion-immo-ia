package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;

/** Historique des versions d'une simulation, de la plus ancienne à la plus récente. */
public interface ObtenirHistoriqueSimulationUseCase {

    List<SimulationRentabilite> obtenirHistorique(SimulationRentabiliteId id, UtilisateurId demandeurId);
}

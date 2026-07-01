package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;

public interface ObtenirMonPortefeuilleUseCase {

    List<LignePortefeuille> obtenir(UtilisateurId proprietaireId);
}

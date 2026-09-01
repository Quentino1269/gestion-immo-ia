package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.rentabilite.domain.LigneComparateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;

public interface ObtenirComparateurUseCase {

    List<LigneComparateur> obtenir(BienId bienId, UtilisateurId demandeurId);
}

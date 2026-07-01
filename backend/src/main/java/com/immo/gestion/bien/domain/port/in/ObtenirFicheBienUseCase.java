package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

public interface ObtenirFicheBienUseCase {

    FicheBien obtenir(BienId bienId, UtilisateurId demandeurId);
}

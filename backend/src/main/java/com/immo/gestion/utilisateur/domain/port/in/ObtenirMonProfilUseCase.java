package com.immo.gestion.utilisateur.domain.port.in;

import com.immo.gestion.utilisateur.domain.ProfilUtilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

public interface ObtenirMonProfilUseCase {

    ProfilUtilisateur obtenir(UtilisateurId utilisateurId);
}

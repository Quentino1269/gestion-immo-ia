package com.immo.gestion.utilisateur.domain.port.in;

import com.immo.gestion.utilisateur.domain.UtilisateurId;

public interface CreerUtilisateurUseCase {

    UtilisateurId creer(CreerUtilisateurCommand commande);
}

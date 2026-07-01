package com.immo.gestion.utilisateur.domain.port.in;

import com.immo.gestion.utilisateur.domain.ProfilUtilisateur;

public interface CompleterMonProfilCivilUseCase {

    ProfilUtilisateur completer(CompleterMonProfilCivilCommand commande);
}

package com.immo.gestion.utilisateur.domain.port.out;

import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;
import com.immo.gestion.shared.MotDePasseSoumis;

public interface HasheurMotDePasse {

    HashMotDePasse hasher(MotDePasseClair motDePasse);

    boolean verifier(HashMotDePasse hash, MotDePasseClair candidat);

    /**
     * Vérifie une soumission arbitraire (login). N'impose pas la politique
     * de longueur applicable à la création. Utilisé aussi pour exécuter le
     * dummy hash anti-énumération du slice authentification.
     */
    boolean verifierSoumission(HashMotDePasse hash, MotDePasseSoumis soumission);
}

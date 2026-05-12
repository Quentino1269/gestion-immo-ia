package com.immo.gestion.utilisateur.domain.port.out;

import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;

public interface HasheurMotDePasse {

    HashMotDePasse hasher(MotDePasseClair motDePasse);

    boolean verifier(HashMotDePasse hash, MotDePasseClair candidat);
}

package com.immo.gestion.utilisateur.domain.port.out;

import com.immo.gestion.shared.Email;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.Optional;

public interface UtilisateurRepository {

    boolean existeParEmail(Email email);

    void enregistrer(Utilisateur utilisateur);

    Optional<Utilisateur> chargerParId(UtilisateurId id);
}

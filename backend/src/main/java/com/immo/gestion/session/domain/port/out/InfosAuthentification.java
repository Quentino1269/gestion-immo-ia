package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.Objects;

/**
 * Projection minimale du read model {@code RepertoireAuthentification}
 * (slice creation-utilisateur §10) consommée par le service d'authentification.
 * Ne contient strictement que ce qui est nécessaire pour vérifier des
 * identifiants et résoudre l'utilisateur — jamais exposée côté API.
 */
public record InfosAuthentification(
        UtilisateurId utilisateurId,
        HashMotDePasse hashMotDePasse,
        StatutCompte statut
) {

    public InfosAuthentification {
        Objects.requireNonNull(utilisateurId, "utilisateurId requis");
        Objects.requireNonNull(hashMotDePasse, "hashMotDePasse requis");
        Objects.requireNonNull(statut, "statut requis");
    }
}

package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;

import java.time.Instant;

/**
 * Évènement publié à l'inscription d'un utilisateur.
 * Cf. docs/slices/creation-utilisateur.md §8.
 * <p>
 * Publié via {@code ApplicationEventPublisher} après persistance. Aucun listener
 * applicatif en V1 ; l'évènement existe pour matérialiser le fait métier et
 * sera consommé par les slices ultérieurs (notifications, journal, etc.).
 */
public record UtilisateurInscrit(
        UtilisateurId utilisateurId,
        Email email,
        HashMotDePasse hashMotDePasse,
        String nom,
        String prenom,
        String telephone,
        StatutCompte statut,
        String versionCgu,
        Instant cguAccepteesLe,
        String versionConfidentialite,
        Instant confidentialiteAccepteeLe,
        Instant inscritLe
) {

    public static UtilisateurInscrit depuis(Utilisateur u) {
        return new UtilisateurInscrit(
                u.id(),
                u.email(),
                u.hashMotDePasse(),
                u.nom(),
                u.prenom(),
                u.telephone(),
                u.statut(),
                u.versionCgu(),
                u.cguAccepteesLe(),
                u.versionConfidentialite(),
                u.confidentialiteAccepteeLe(),
                u.inscritLe()
        );
    }
}

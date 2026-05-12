package com.immo.gestion.utilisateur.domain.port.in;

/**
 * Commande de création d'un utilisateur. Cf. docs/slices/creation-utilisateur.md §7.
 * Les VO (Email, MotDePasseClair) sont construits par le service applicatif.
 */
public record CreerUtilisateurCommand(
        String email,
        String motDePasseClair,
        String nom,
        String prenom,
        String telephone,
        boolean accepteCgu,
        boolean accepteConfidentialite
) {
}

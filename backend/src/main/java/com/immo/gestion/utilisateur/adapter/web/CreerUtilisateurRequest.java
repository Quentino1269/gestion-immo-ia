package com.immo.gestion.utilisateur.adapter.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload HTTP de l'inscription. Validation Bean Validation pour les contraintes
 * non métier ; la validation domaine se fait dans le service via les VO.
 */
public record CreerUtilisateurRequest(
        @NotBlank String email,
        @NotBlank @Size(min = 12, max = 256) String motDePasse,
        @NotBlank @Size(max = 80) String nom,
        @NotBlank @Size(max = 80) String prenom,
        @Size(max = 20) String telephone,
        @AssertTrue(message = "vous devez accepter les CGU") boolean accepteCgu,
        @AssertTrue(message = "vous devez accepter la politique de confidentialité") boolean accepteConfidentialite
) {
}

package com.immo.gestion.utilisateur.domain;

/**
 * Statut d'un compte utilisateur.
 * V1 : seul {@link #ACTIF} est utilisé (cf. docs/slices/creation-utilisateur.md D3).
 * Les autres valeurs sont préparées pour les slices futurs (suspension, suppression, vérif email).
 */
public enum StatutCompte {
    ACTIF,
    SUSPENDU,
    SUPPRIME
}

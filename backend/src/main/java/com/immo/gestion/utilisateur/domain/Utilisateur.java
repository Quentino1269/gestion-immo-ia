package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Aggregate Utilisateur — record immuable. Invariants vérifiés en compact constructor.
 * Cf. docs/slices/creation-utilisateur.md §9 (I-1..I-6).
 */
public record Utilisateur(
        UtilisateurId id,
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

    private static final int LONGUEUR_NOM_MAX = 80;
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    // Tolère lettres Unicode + espaces + apostrophes + tirets + points
    private static final Pattern NOM_PRENOM =
            Pattern.compile("^[\\p{L}][\\p{L}\\s.'-]*$");

    public Utilisateur {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(email, "email requis");
        Objects.requireNonNull(hashMotDePasse, "hash requis");
        Objects.requireNonNull(statut, "statut requis");
        Objects.requireNonNull(versionCgu, "versionCgu requise");
        Objects.requireNonNull(cguAccepteesLe, "cguAccepteesLe requis");
        Objects.requireNonNull(versionConfidentialite, "versionConfidentialite requise");
        Objects.requireNonNull(confidentialiteAccepteeLe, "confidentialiteAccepteeLe requis");
        Objects.requireNonNull(inscritLe, "inscritLe requis");

        nom = exigerNomValide(nom, "nom");
        prenom = exigerNomValide(prenom, "prenom");

        if (telephone != null) {
            String t = telephone.trim();
            if (t.isEmpty()) {
                telephone = null;
            } else {
                if (!E164.matcher(t).matches()) {
                    throw new IllegalArgumentException("telephone non E.164");
                }
                telephone = t;
            }
        }
    }

    private static String exigerNomValide(String valeur, String label) {
        Objects.requireNonNull(valeur, label + " requis");
        String trim = valeur.trim();
        if (trim.isEmpty()) {
            throw new IllegalArgumentException(label + " vide");
        }
        if (trim.length() > LONGUEUR_NOM_MAX) {
            throw new IllegalArgumentException(label + " trop long");
        }
        if (!NOM_PRENOM.matcher(trim).matches()) {
            throw new IllegalArgumentException(label + " contient des caractères invalides");
        }
        return trim;
    }

    public Optional<String> telephoneOpt() {
        return Optional.ofNullable(telephone);
    }
}

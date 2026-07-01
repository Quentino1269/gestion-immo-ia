package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.domain.port.in.ModificationProfilRefuseeException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Aggregate Utilisateur — étendu par le slice Enrichissement du Profil.
 * Invariants slice création : I-1..I-6.
 * Invariants slice enrichissement : I-1..I-11.
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
        Instant inscritLe,
        // Profil civil (slice enrichissement)
        Civilite civilite,
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        Adresse adresseDomicile,
        StatutProfil statutProfil,
        Instant profilCompleteLe
) {

    private static final int LONGUEUR_NOM_MAX = 80;
    private static final int LIEU_VILLE_MAX = 80;
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern NOM_PRENOM = Pattern.compile("^[\\p{L}][\\p{L}\\s.'-]*$");
    private static final Set<String> PAYS_VALIDES =
            Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

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
        Objects.requireNonNull(statutProfil, "statutProfil requis");

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

        // Profil civil — validations statiques uniquement (pas d'accès à Clock)
        if (dateNaissance != null) {
            // I-3 : plausibilité
            if (dateNaissance.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("dateNaissance antérieure à 1900-01-01");
            }
        }
        if (lieuNaissanceVille != null) {
            lieuNaissanceVille = lieuNaissanceVille.trim();
            if (lieuNaissanceVille.isEmpty() || lieuNaissanceVille.length() > LIEU_VILLE_MAX) {
                throw new IllegalArgumentException("lieuNaissanceVille invalide");
            }
        }
        if (lieuNaissancePaysIso != null) {
            lieuNaissancePaysIso = lieuNaissancePaysIso.trim().toUpperCase();
            if (!PAYS_VALIDES.contains(lieuNaissancePaysIso)) {
                throw new IllegalArgumentException("lieuNaissancePaysIso invalide");
            }
        }
        if (nationaliteIso != null) {
            nationaliteIso = nationaliteIso.trim().toUpperCase();
            if (!PAYS_VALIDES.contains(nationaliteIso)) {
                throw new IllegalArgumentException("nationaliteIso invalide");
            }
        }
    }

    /**
     * Traite la commande CompleterMonProfilCivil.
     * Applique I-2, I-3, I-4..I-11 du slice enrichissement-profil.
     * Retourne l'utilisateur mis à jour et la liste d'événements à publier.
     */
    public ResultatCompletionProfil completerProfil(
            com.immo.gestion.utilisateur.domain.port.in.CompleterMonProfilCivilCommand cmd,
            Instant maintenant
    ) {
        List<Object> evenements = new ArrayList<>();

        // --- Validation des invariants temporels ---
        if (cmd.dateNaissance() != null) {
            LocalDate aujourd_hui = LocalDate.ofInstant(maintenant, java.time.ZoneOffset.UTC);
            // I-3
            if (cmd.dateNaissance().isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException("dateNaissance antérieure à 1900-01-01");
            }
            // I-2
            if (Period.between(cmd.dateNaissance(), aujourd_hui).getYears() < 18) {
                throw new IllegalArgumentException("dateNaissance : l'utilisateur doit être majeur (18 ans révolus)");
            }
        }

        // --- Détection des modifications (I-10) ---
        detecterModification("civilite", civilite, cmd.civilite());
        detecterModification("dateNaissance", dateNaissance, cmd.dateNaissance());
        detecterModification("lieuNaissanceVille",
                lieuNaissanceVille,
                cmd.lieuNaissanceVille() != null ? cmd.lieuNaissanceVille().trim() : null);
        detecterModification("lieuNaissancePaysIso",
                lieuNaissancePaysIso,
                cmd.lieuNaissancePaysIso() != null ? cmd.lieuNaissancePaysIso().toUpperCase() : null);
        detecterModification("nationaliteIso",
                nationaliteIso,
                cmd.nationaliteIso() != null ? cmd.nationaliteIso().toUpperCase() : null);
        detecterModification("adresseDomicile", adresseDomicile, cmd.adresseDomicile());
        if (telephone != null && cmd.telephone() != null) {
            String telCmd = cmd.telephone().trim();
            if (!telephone.equals(telCmd)) {
                throw new ModificationProfilRefuseeException("telephone");
            }
        }

        // --- Calcul du nouvel état ---
        Civilite nouvelleCivilite = cmd.civilite() != null ? cmd.civilite() : civilite;
        LocalDate nouvelleDateNaissance = cmd.dateNaissance() != null ? cmd.dateNaissance() : dateNaissance;
        String nouveauLieuVille = cmd.lieuNaissanceVille() != null ? cmd.lieuNaissanceVille().trim() : lieuNaissanceVille;
        String nouveauLieuPays = cmd.lieuNaissancePaysIso() != null ? cmd.lieuNaissancePaysIso().toUpperCase() : lieuNaissancePaysIso;
        String nouvelleNationalite = cmd.nationaliteIso() != null ? cmd.nationaliteIso().toUpperCase() : nationaliteIso;
        Adresse nouvelleAdresse = cmd.adresseDomicile() != null ? cmd.adresseDomicile() : adresseDomicile;
        String nouveauTelephone = (cmd.telephone() != null && telephone == null)
                ? cmd.telephone().trim()
                : telephone;

        // --- Émission des événements fins ---
        if (cmd.civilite() != null && civilite == null) {
            evenements.add(new CiviliteRenseignee(id, nouvelleCivilite, maintenant));
        }

        boolean naissanceCompleteAvant = dateNaissance != null && lieuNaissanceVille != null && lieuNaissancePaysIso != null;
        boolean naissanceCompleteApres = nouvelleDateNaissance != null && nouveauLieuVille != null && nouveauLieuPays != null;
        if (naissanceCompleteApres && !naissanceCompleteAvant) {
            evenements.add(new DonneesNaissanceRenseignees(id, nouvelleDateNaissance, nouveauLieuVille, nouveauLieuPays, nouvelleNationalite, maintenant));
        }

        if (cmd.adresseDomicile() != null && adresseDomicile == null) {
            evenements.add(new AdresseDomicileRenseignee(id, nouvelleAdresse, maintenant));
        }

        if (cmd.telephone() != null && telephone == null) {
            evenements.add(new TelephoneRenseigne(id, nouveauTelephone, maintenant));
        }

        // Bascule MINIMAL → COMPLET (I-11, D8)
        boolean profilCompletAvant = statutProfil == StatutProfil.COMPLET;
        boolean conditionsD8 = naissanceCompleteApres && nouvelleAdresse != null;
        StatutProfil nouveauStatut = conditionsD8 ? StatutProfil.COMPLET : StatutProfil.MINIMAL;
        Instant nouveauCompleteLe = profilCompleteLe;
        if (conditionsD8 && !profilCompletAvant) {
            nouveauCompleteLe = maintenant;
            evenements.add(new ProfilUtilisateurComplete(id, maintenant));
        }

        Utilisateur misAJour = new Utilisateur(
                id, email, hashMotDePasse, nom, prenom, nouveauTelephone,
                statut, versionCgu, cguAccepteesLe, versionConfidentialite, confidentialiteAccepteeLe, inscritLe,
                nouvelleCivilite, nouvelleDateNaissance, nouveauLieuVille, nouveauLieuPays, nouvelleNationalite,
                nouvelleAdresse, nouveauStatut, nouveauCompleteLe
        );

        return new ResultatCompletionProfil(misAJour, Collections.unmodifiableList(evenements));
    }

    public Optional<String> telephoneOpt() {
        return Optional.ofNullable(telephone);
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

    private static <T> void detecterModification(String champ, T actuel, T nouveau) {
        if (nouveau == null || actuel == null) return;
        if (!actuel.equals(nouveau)) {
            throw new ModificationProfilRefuseeException(champ);
        }
    }
}

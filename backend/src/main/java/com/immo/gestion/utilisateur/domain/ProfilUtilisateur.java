package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.Email;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Read model du profil utilisateur — projeté depuis l'état courant de l'aggregate Utilisateur.
 * Cf. docs/slices/enrichissement-profil.md §10.
 */
public record ProfilUtilisateur(
        UtilisateurId utilisateurId,
        String nom,
        String prenom,
        Civilite civilite,
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        Email email,
        String telephone,
        Adresse adresseDomicile,
        StatutProfil statutProfil,
        Instant profilCompleteLe,
        List<String> champsManquantsPourBail
) {

    public static ProfilUtilisateur depuis(Utilisateur u) {
        List<String> manquants = new ArrayList<>();
        if (u.dateNaissance() == null || u.lieuNaissanceVille() == null || u.lieuNaissancePaysIso() == null) {
            manquants.add("dateNaissance");
        }
        if (u.adresseDomicile() == null) {
            manquants.add("adresseDomicile");
        }
        return new ProfilUtilisateur(
                u.id(),
                u.nom(),
                u.prenom(),
                u.civilite(),
                u.dateNaissance(),
                u.lieuNaissanceVille(),
                u.lieuNaissancePaysIso(),
                u.nationaliteIso(),
                u.email(),
                u.telephone(),
                u.adresseDomicile(),
                u.statutProfil(),
                u.profilCompleteLe(),
                List.copyOf(manquants)
        );
    }
}

package com.immo.gestion.utilisateur.adapter.web;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.Civilite;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateur;
import com.immo.gestion.utilisateur.domain.StatutProfil;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProfilResponse(
        UUID utilisateurId,
        Identite identite,
        Coordonnees coordonnees,
        AdresseResponse adresseDomicile,
        StatutProfil statutProfil,
        List<String> champsManquantsPourBail
) {

    public record Identite(
            String nom,
            String prenom,
            Civilite civilite,
            LocalDate dateNaissance,
            String lieuNaissanceVille,
            String lieuNaissancePaysIso,
            String nationaliteIso
    ) {}

    public record Coordonnees(String email, String telephone) {}

    public record AdresseResponse(
            String numero,
            String voie,
            String complement,
            String codePostal,
            String commune,
            String paysIso
    ) {}

    public static ProfilResponse depuis(ProfilUtilisateur profil) {
        Adresse adr = profil.adresseDomicile();
        AdresseResponse adresseReponse = adr == null ? null : new AdresseResponse(
                adr.numero(), adr.voie(), adr.complement(),
                adr.codePostal(), adr.commune(), adr.paysIso()
        );
        return new ProfilResponse(
                profil.utilisateurId().valeur(),
                new Identite(
                        profil.nom(), profil.prenom(), profil.civilite(),
                        profil.dateNaissance(), profil.lieuNaissanceVille(),
                        profil.lieuNaissancePaysIso(), profil.nationaliteIso()
                ),
                new Coordonnees(profil.email().valeur(), profil.telephone()),
                adresseReponse,
                profil.statutProfil(),
                profil.champsManquantsPourBail()
        );
    }
}

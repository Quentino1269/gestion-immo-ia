package com.immo.gestion.utilisateur.domain.port.in;

import com.immo.gestion.utilisateur.domain.Adresse;
import com.immo.gestion.utilisateur.domain.Civilite;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.LocalDate;

public record CompleterMonProfilCivilCommand(
        UtilisateurId utilisateurId,
        Civilite civilite,
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        Adresse adresseDomicile,
        String telephone
) {}

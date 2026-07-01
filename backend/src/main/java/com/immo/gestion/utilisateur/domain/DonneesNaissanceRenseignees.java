package com.immo.gestion.utilisateur.domain;

import java.time.Instant;
import java.time.LocalDate;

public record DonneesNaissanceRenseignees(
        UtilisateurId utilisateurId,
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        Instant renseigneLe
) {}

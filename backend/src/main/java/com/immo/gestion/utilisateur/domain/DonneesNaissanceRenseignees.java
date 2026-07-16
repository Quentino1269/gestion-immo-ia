package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;

public record DonneesNaissanceRenseignees(
        UtilisateurId utilisateurId,
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        Instant survenuLe
) implements DomainEvent {}

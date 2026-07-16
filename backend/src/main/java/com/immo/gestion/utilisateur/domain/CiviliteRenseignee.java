package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

public record CiviliteRenseignee(
        UtilisateurId utilisateurId,
        Civilite civilite,
        Instant survenuLe
) implements DomainEvent {}

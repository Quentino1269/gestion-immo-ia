package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

public record TelephoneRenseigne(
        UtilisateurId utilisateurId,
        String telephone,
        Instant survenuLe
) implements DomainEvent {}

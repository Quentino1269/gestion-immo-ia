package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

public record ProfilUtilisateurComplete(
        UtilisateurId utilisateurId,
        Instant survenuLe
) implements DomainEvent {}

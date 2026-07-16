package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.domain.DomainEvent;

import java.util.List;

public record ResultatCompletionProfil(
        Utilisateur misAJour,
        List<DomainEvent> evenements
) {}

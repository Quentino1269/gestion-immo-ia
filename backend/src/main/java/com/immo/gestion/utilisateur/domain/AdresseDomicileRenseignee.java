package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;

import java.time.Instant;

public record AdresseDomicileRenseignee(
        UtilisateurId utilisateurId,
        Adresse adresseDomicile,
        Instant survenuLe
) implements DomainEvent {}

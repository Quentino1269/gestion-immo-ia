package com.immo.gestion.utilisateur.domain;

import java.time.Instant;

public record AdresseDomicileRenseignee(
        UtilisateurId utilisateurId,
        Adresse adresseDomicile,
        Instant renseigneLe
) {}

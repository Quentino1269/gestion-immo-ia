package com.immo.gestion.utilisateur.domain;

import java.time.Instant;

public record TelephoneRenseigne(
        UtilisateurId utilisateurId,
        String telephone,
        Instant renseigneLe
) {}

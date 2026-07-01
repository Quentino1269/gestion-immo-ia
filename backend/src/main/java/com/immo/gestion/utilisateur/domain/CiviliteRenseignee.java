package com.immo.gestion.utilisateur.domain;

import java.time.Instant;

public record CiviliteRenseignee(
        UtilisateurId utilisateurId,
        Civilite civilite,
        Instant renseigneLe
) {}

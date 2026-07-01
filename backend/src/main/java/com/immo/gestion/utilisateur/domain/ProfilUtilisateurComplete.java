package com.immo.gestion.utilisateur.domain;

import java.time.Instant;

public record ProfilUtilisateurComplete(
        UtilisateurId utilisateurId,
        Instant completeLe
) {}

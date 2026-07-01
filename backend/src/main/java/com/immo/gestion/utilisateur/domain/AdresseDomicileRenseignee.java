package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Adresse;
import java.time.Instant;

public record AdresseDomicileRenseignee(
        UtilisateurId utilisateurId,
        Adresse adresseDomicile,
        Instant renseigneLe
) {}

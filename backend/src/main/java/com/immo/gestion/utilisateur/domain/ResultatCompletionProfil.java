package com.immo.gestion.utilisateur.domain;

import java.util.List;

public record ResultatCompletionProfil(
        Utilisateur misAJour,
        List<Object> evenements
) {}

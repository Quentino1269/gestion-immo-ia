package com.immo.gestion.utilisateur.adapter.web;

import java.util.List;

public record ApiError(String message, List<ChampEnErreur> erreurs) {

    public static ApiError simple(String message) {
        return new ApiError(message, List.of());
    }

    public record ChampEnErreur(String champ, String message) {}
}

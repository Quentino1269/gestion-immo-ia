package com.immo.gestion.utilisateur.adapter.web;

public record AdresseRequest(
        String numero,
        String voie,
        String complement,
        String codePostal,
        String commune,
        String paysIso
) {}

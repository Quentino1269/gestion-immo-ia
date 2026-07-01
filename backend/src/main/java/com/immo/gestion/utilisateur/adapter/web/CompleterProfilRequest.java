package com.immo.gestion.utilisateur.adapter.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.immo.gestion.utilisateur.domain.Civilite;

import java.time.LocalDate;

public record CompleterProfilRequest(
        Civilite civilite,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateNaissance,
        String lieuNaissanceVille,
        String lieuNaissancePaysIso,
        String nationaliteIso,
        AdresseRequest adresseDomicile,
        String telephone
) {}

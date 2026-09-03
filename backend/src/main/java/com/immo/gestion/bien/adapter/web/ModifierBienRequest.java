package com.immo.gestion.bien.adapter.web;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record ModifierBienRequest(
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        boolean meuble,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate disponibleAPartirDu,
        String libelleChambre,
        int nbPiecesPrincipales
) {
}

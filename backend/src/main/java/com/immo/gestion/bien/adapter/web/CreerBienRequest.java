package com.immo.gestion.bien.adapter.web;

import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerBienRequest(
        TypeBien typeBien,
        UUID bienParentId,
        String libelleChambre,
        int nbPiecesPrincipales,
        BigDecimal surfaceM2,
        boolean meuble,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        AdresseRequest adresse,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate disponibleAPartirDu
) {

    public record AdresseRequest(
            String numero,
            String voie,
            String complement,
            String codePostal,
            String commune,
            String paysIso
    ) {}
}

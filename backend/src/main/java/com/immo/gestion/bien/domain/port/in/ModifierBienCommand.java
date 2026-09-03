package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.LocalDate;

public record ModifierBienCommand(
        BienId bienId,
        UtilisateurId demandeurId,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        boolean meuble,
        LocalDate disponibleAPartirDu,
        String libelleChambre,
        int nbPiecesPrincipales
) {
}

package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreerBienCommand(
        UtilisateurId proprietaireId,
        TypeBien typeBien,
        BienId bienParentId,
        String libelleChambre,
        int nbPiecesPrincipales,
        BigDecimal surfaceM2,
        boolean meuble,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        Adresse adresse,
        LocalDate disponibleAPartirDu
) {
}

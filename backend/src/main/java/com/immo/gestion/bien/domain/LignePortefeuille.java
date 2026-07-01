package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read model — ligne dans la liste du portefeuille. Cf. §10. */
public record LignePortefeuille(
        BienId bienId,
        TypeBien typeBien,
        String libelleCommercial,
        BigDecimal surfaceM2,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        Adresse adresse,
        LocalDate disponibleAPartirDu
) {

    public static LignePortefeuille depuis(Bien bien) {
        return new LignePortefeuille(
                bien.id(),
                bien.typeBien(),
                bien.libelleCommercial(),
                bien.surfaceM2(),
                bien.loyerHorsChargesEnCentimes(),
                bien.chargesEnCentimes(),
                bien.modaliteCharges(),
                bien.adresse(),
                bien.disponibleAPartirDu()
        );
    }
}

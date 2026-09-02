package com.immo.gestion.bien.adapter.web;

import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.shared.Adresse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LignePortefeuilleResponse(
        UUID bienId,
        TypeBien typeBien,
        String libelleCommercial,
        UUID bienParentId,
        BigDecimal surfaceM2,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        FicheBienResponse.AdresseResponse adresse,
        LocalDate disponibleAPartirDu
) {

    public static LignePortefeuilleResponse depuis(LignePortefeuille ligne) {
        Adresse adr = ligne.adresse();
        return new LignePortefeuilleResponse(
                ligne.bienId().valeur(),
                ligne.typeBien(),
                ligne.libelleCommercial(),
                ligne.bienParentId() != null ? ligne.bienParentId().valeur() : null,
                ligne.surfaceM2(),
                ligne.loyerHorsChargesEnCentimes(),
                ligne.chargesEnCentimes(),
                ligne.modaliteCharges(),
                new FicheBienResponse.AdresseResponse(adr.numero(), adr.voie(), adr.complement(),
                        adr.codePostal(), adr.commune(), adr.paysIso()),
                ligne.disponibleAPartirDu()
        );
    }
}

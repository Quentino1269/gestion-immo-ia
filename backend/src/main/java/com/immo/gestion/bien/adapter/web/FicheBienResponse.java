package com.immo.gestion.bien.adapter.web;

import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.shared.Adresse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FicheBienResponse(
        UUID bienId,
        TypeBien typeBien,
        String libelleCommercial,
        UUID bienParentId,
        String libelleChambre,
        int nbPiecesPrincipales,
        BigDecimal surfaceM2,
        boolean meuble,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        AdresseResponse adresse,
        LocalDate disponibleAPartirDu,
        Instant ajouteLe
) {

    public static FicheBienResponse depuis(FicheBien fiche) {
        Adresse adr = fiche.adresse();
        return new FicheBienResponse(
                fiche.bienId().valeur(),
                fiche.typeBien(),
                fiche.libelleCommercial(),
                fiche.bienParentId() != null ? fiche.bienParentId().valeur() : null,
                fiche.libelleChambre(),
                fiche.nbPiecesPrincipales(),
                fiche.surfaceM2(),
                fiche.meuble(),
                fiche.loyerHorsChargesEnCentimes(),
                fiche.chargesEnCentimes(),
                fiche.modaliteCharges(),
                new AdresseResponse(adr.numero(), adr.voie(), adr.complement(),
                        adr.codePostal(), adr.commune(), adr.paysIso()),
                fiche.disponibleAPartirDu(),
                fiche.ajouteLe()
        );
    }

    public record AdresseResponse(
            String numero,
            String voie,
            String complement,
            String codePostal,
            String commune,
            String paysIso
    ) {}
}

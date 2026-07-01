package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Read model — vue détaillée d'un bien. Cf. §10. */
public record FicheBien(
        BienId bienId,
        UtilisateurId proprietaireInitialId,
        TypeBien typeBien,
        String libelleCommercial,
        BienId bienParentId,
        String libelleChambre,
        int nbPiecesPrincipales,
        BigDecimal surfaceM2,
        boolean meuble,
        long loyerHorsChargesEnCentimes,
        long chargesEnCentimes,
        ModaliteCharges modaliteCharges,
        Adresse adresse,
        LocalDate disponibleAPartirDu,
        Instant ajouteLe
) {

    public static FicheBien depuis(Bien bien) {
        return new FicheBien(
                bien.id(),
                bien.proprietaireInitialId(),
                bien.typeBien(),
                bien.libelleCommercial(),
                bien.bienParentId(),
                bien.libelleChambre(),
                bien.nbPiecesPrincipales(),
                bien.surfaceM2(),
                bien.meuble(),
                bien.loyerHorsChargesEnCentimes(),
                bien.chargesEnCentimes(),
                bien.modaliteCharges(),
                bien.adresse(),
                bien.disponibleAPartirDu(),
                bien.ajouteLe()
        );
    }
}

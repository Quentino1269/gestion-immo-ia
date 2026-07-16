package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Événement émis quand un bien est créé dans le portefeuille.
 * Cf. docs/slices/creation-bien.md §8.
 */
public record BienAjouteAuPortefeuille(
        BienId bienId,
        UtilisateurId proprietaireInitialId,
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
        LocalDate disponibleAPartirDu,
        Instant survenuLe
) implements DomainEvent {

    public static BienAjouteAuPortefeuille depuis(Bien bien) {
        return new BienAjouteAuPortefeuille(
                bien.id(),
                bien.proprietaireInitialId(),
                bien.typeBien(),
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

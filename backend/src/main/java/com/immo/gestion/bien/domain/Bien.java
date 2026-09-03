package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Bien — invariants I-1..I-8, I-COLOC-1..I-COLOC-3, I-MOD-1..I-MOD-9.
 * Les invariants cross-aggregate (I-COLOC-2, I-COLOC-4, I-COLOC-5, I-MOD-1, I-MOD-2, I-MOD-6)
 * sont vérifiés par BienService avant construction.
 * Cf. docs/slices/creation-bien.md §9, docs/slices/modification-bien.md §9.
 */
public record Bien(
        BienId id,
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
        Instant ajouteLe
) {

    private static final int LIBELLE_CHAMBRE_MAX = 50;

    public Bien {
        // I-1
        Objects.requireNonNull(proprietaireInitialId, "proprietaireInitialId requis");
        // I-2
        Objects.requireNonNull(typeBien, "typeBien requis");
        // I-3
        if (nbPiecesPrincipales < 1) {
            throw new IllegalArgumentException("nbPiecesPrincipales doit être ≥ 1");
        }
        // I-4
        Objects.requireNonNull(surfaceM2, "surfaceM2 requise");
        if (surfaceM2.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("surfaceM2 doit être > 0");
        }
        if (surfaceM2.scale() > 2) {
            throw new IllegalArgumentException("surfaceM2 : 2 décimales maximum");
        }
        // I-5
        if (loyerHorsChargesEnCentimes < 0) {
            throw new IllegalArgumentException("loyerHorsChargesEnCentimes doit être ≥ 0");
        }
        // I-6
        if (chargesEnCentimes < 0) {
            throw new IllegalArgumentException("chargesEnCentimes doit être ≥ 0");
        }
        // I-7
        Objects.requireNonNull(disponibleAPartirDu, "disponibleAPartirDu requise");
        // I-8
        Objects.requireNonNull(adresse, "adresse requise");

        // modaliteCharges cohérente (I-CHARGES-1)
        Objects.requireNonNull(modaliteCharges, "modaliteCharges requise");
        ModaliteCharges attendue = meuble ? ModaliteCharges.FORFAIT : ModaliteCharges.PROVISION;
        if (modaliteCharges != attendue) {
            throw new IllegalArgumentException("modaliteCharges incohérente avec meuble");
        }

        // I-COLOC-1 : bienParentId et libelleChambre présents ⟺ CHAMBRE_COLOCATION
        if (typeBien == TypeBien.CHAMBRE_COLOCATION) {
            Objects.requireNonNull(bienParentId, "bienParentId requis pour une CHAMBRE_COLOCATION");
            if (libelleChambre == null || libelleChambre.isBlank()) {
                throw new IllegalArgumentException("libelleChambre requis pour une CHAMBRE_COLOCATION");
            }
        } else {
            if (bienParentId != null) {
                throw new IllegalArgumentException("bienParentId interdit pour ce type de bien");
            }
            if (libelleChambre != null) {
                throw new IllegalArgumentException("libelleChambre interdit pour ce type de bien");
            }
        }

        // Normalisation et validation libellé chambre
        if (libelleChambre != null) {
            libelleChambre = libelleChambre.strip();
            if (libelleChambre.length() > LIBELLE_CHAMBRE_MAX) {
                throw new IllegalArgumentException("libelleChambre trop long (max " + LIBELLE_CHAMBRE_MAX + " car.)");
            }
        }
    }

    /** Calcule l'étiquette commerciale affichée dans les read models (D4, §11). */
    public String libelleCommercial() {
        return switch (typeBien) {
            case MAISON -> "Maison";
            case CHAMBRE_COLOCATION -> "Chambre en colocation — " + libelleChambre;
            case APPARTEMENT -> nbPiecesPrincipales >= 6 ? "T6+" : "T" + nbPiecesPrincipales;
        };
    }

    /**
     * Reconstruit l'état courant par rejeu du flux (Event Sourcing, MISSION §5) : un
     * {@link BienAjouteAuPortefeuille} initial, puis zéro ou plusieurs événements de révision
     * (cf. docs/slices/modification-bien.md §8) qui écrasent le(s) champ(s) concerné(s).
     */
    public static Bien reconstruire(List<DomainEvent> evenements) {
        if (evenements.isEmpty()) {
            throw new IllegalStateException("Flux Bien vide : impossible de reconstruire l'aggregate");
        }
        Bien etat = null;
        for (DomainEvent evenement : evenements) {
            etat = appliquer(etat, evenement);
        }
        return etat;
    }

    private static Bien appliquer(Bien etat, DomainEvent evenement) {
        if (etat == null && !(evenement instanceof BienAjouteAuPortefeuille)) {
            throw new IllegalStateException(
                    "Flux Bien corrompu : premier événement attendu BienAjouteAuPortefeuille, reçu "
                            + evenement.getClass());
        }
        return switch (evenement) {
            case BienAjouteAuPortefeuille e -> new Bien(
                    e.bienId(), e.proprietaireInitialId(), e.typeBien(), e.bienParentId(), e.libelleChambre(),
                    e.nbPiecesPrincipales(), e.surfaceM2(), e.meuble(), e.loyerHorsChargesEnCentimes(),
                    e.chargesEnCentimes(), e.modaliteCharges(), e.adresse(), e.disponibleAPartirDu(), e.survenuLe()
            );
            case LoyerRevise e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    etat.nbPiecesPrincipales, etat.surfaceM2, etat.meuble, e.loyerHorsChargesEnCentimes(),
                    etat.chargesEnCentimes, etat.modaliteCharges, etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            case ChargesRevisees e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    etat.nbPiecesPrincipales, etat.surfaceM2, etat.meuble, etat.loyerHorsChargesEnCentimes,
                    e.chargesEnCentimes(), etat.modaliteCharges, etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            case MeubleEntreDansLeLogement e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    etat.nbPiecesPrincipales, etat.surfaceM2, true, etat.loyerHorsChargesEnCentimes,
                    etat.chargesEnCentimes, e.modaliteCharges(), etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            case LogementDevenuNu e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    etat.nbPiecesPrincipales, etat.surfaceM2, false, etat.loyerHorsChargesEnCentimes,
                    etat.chargesEnCentimes, e.modaliteCharges(), etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            case DisponibiliteRevisee e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    etat.nbPiecesPrincipales, etat.surfaceM2, etat.meuble, etat.loyerHorsChargesEnCentimes,
                    etat.chargesEnCentimes, etat.modaliteCharges, etat.adresse, e.disponibleAPartirDu(), etat.ajouteLe
            );
            case LibelleChambreRenomme e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, e.libelleChambre(),
                    etat.nbPiecesPrincipales, etat.surfaceM2, etat.meuble, etat.loyerHorsChargesEnCentimes,
                    etat.chargesEnCentimes, etat.modaliteCharges, etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            case NombrePiecesRevise e -> new Bien(
                    etat.id, etat.proprietaireInitialId, etat.typeBien, etat.bienParentId, etat.libelleChambre,
                    e.nbPiecesPrincipales(), etat.surfaceM2, etat.meuble, etat.loyerHorsChargesEnCentimes,
                    etat.chargesEnCentimes, etat.modaliteCharges, etat.adresse, etat.disponibleAPartirDu, etat.ajouteLe
            );
            default -> throw new IllegalStateException(
                    "Événement inattendu dans le flux Bien : " + evenement.getClass());
        };
    }
}

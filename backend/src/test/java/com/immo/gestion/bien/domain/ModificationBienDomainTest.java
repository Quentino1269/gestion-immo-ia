package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModificationBienDomainTest {

    private static final UtilisateurId PROPRIETAIRE = UtilisateurId.nouveau();
    private static final Adresse ADRESSE = new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    private static final LocalDate DISPO = LocalDate.of(2026, 8, 1);
    private static final Instant T_CREATION = Instant.parse("2026-07-01T10:00:00Z");
    private static final Instant T_MODIF = Instant.parse("2026-09-03T09:00:00Z");

    private BienAjouteAuPortefeuille evenementCreationAppartement(BienId id) {
        return new BienAjouteAuPortefeuille(
                id, PROPRIETAIRE, TypeBien.APPARTEMENT, null, null,
                3, new BigDecimal("55.00"), false,
                80000L, 5000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, T_CREATION
        );
    }

    @Test
    void reconstruire_flux_vide_leve_exception() {
        assertThatThrownBy(() -> Bien.reconstruire(List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstruire_premier_evenement_inattendu_leve_exception() {
        BienId id = BienId.nouveau();
        DomainEvent premier = new LoyerRevise(id, 90000L, T_MODIF);

        assertThatThrownBy(() -> Bien.reconstruire(List.of(premier)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstruire_seul_evenement_creation_retourne_etat_initial() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(evenementCreationAppartement(id)));

        assertThat(b.loyerHorsChargesEnCentimes()).isEqualTo(80000L);
        assertThat(b.chargesEnCentimes()).isEqualTo(5000L);
        assertThat(b.nbPiecesPrincipales()).isEqualTo(3);
    }

    @Test
    void reconstruire_applique_loyer_revise() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new LoyerRevise(id, 95000L, T_MODIF)
        ));

        assertThat(b.loyerHorsChargesEnCentimes()).isEqualTo(95000L);
        assertThat(b.chargesEnCentimes()).isEqualTo(5000L); // inchangé
    }

    @Test
    void reconstruire_applique_charges_revisees() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new ChargesRevisees(id, 7000L, T_MODIF)
        ));

        assertThat(b.chargesEnCentimes()).isEqualTo(7000L);
    }

    @Test
    void reconstruire_applique_meuble_entre_et_derive_modalite_forfait() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new MeubleEntreDansLeLogement(id, ModaliteCharges.FORFAIT, T_MODIF)
        ));

        assertThat(b.meuble()).isTrue();
        assertThat(b.modaliteCharges()).isEqualTo(ModaliteCharges.FORFAIT);
    }

    @Test
    void reconstruire_applique_disponibilite_revisee() {
        BienId id = BienId.nouveau();
        LocalDate nouvelleDate = LocalDate.of(2026, 12, 1);
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new DisponibiliteRevisee(id, nouvelleDate, T_MODIF)
        ));

        assertThat(b.disponibleAPartirDu()).isEqualTo(nouvelleDate);
    }

    @Test
    void reconstruire_applique_nombre_pieces_revise() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new NombrePiecesRevise(id, 4, T_MODIF)
        ));

        assertThat(b.nbPiecesPrincipales()).isEqualTo(4);
        assertThat(b.libelleCommercial()).isEqualTo("T4");
    }

    @Test
    void reconstruire_applique_libelle_chambre_renomme() {
        BienId parentId = BienId.nouveau();
        BienId id = BienId.nouveau();
        BienAjouteAuPortefeuille creation = new BienAjouteAuPortefeuille(
                id, PROPRIETAIRE, TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A",
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, T_CREATION
        );
        Bien b = Bien.reconstruire(List.of(
                creation,
                new LibelleChambreRenomme(id, "Chambre côté cour", T_MODIF)
        ));

        assertThat(b.libelleChambre()).isEqualTo("Chambre côté cour");
        assertThat(b.libelleCommercial()).isEqualTo("Chambre en colocation — Chambre côté cour");
    }

    @Test
    void reconstruire_applique_plusieurs_revisions_successives() {
        BienId id = BienId.nouveau();
        Bien b = Bien.reconstruire(List.of(
                evenementCreationAppartement(id),
                new LoyerRevise(id, 90000L, T_MODIF),
                new ChargesRevisees(id, 6000L, T_MODIF),
                new NombrePiecesRevise(id, 4, T_MODIF)
        ));

        assertThat(b.loyerHorsChargesEnCentimes()).isEqualTo(90000L);
        assertThat(b.chargesEnCentimes()).isEqualTo(6000L);
        assertThat(b.nbPiecesPrincipales()).isEqualTo(4);
        assertThat(b.id()).isEqualTo(id);
        assertThat(b.proprietaireInitialId()).isEqualTo(PROPRIETAIRE);
    }
}

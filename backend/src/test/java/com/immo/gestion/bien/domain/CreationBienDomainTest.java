package com.immo.gestion.bien.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreationBienDomainTest {

    private static final UtilisateurId PROPRIETAIRE = UtilisateurId.nouveau();
    private static final Adresse ADRESSE = new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    private static final LocalDate DISPO = LocalDate.of(2026, 8, 1);
    private static final Instant MAINTENANT = Instant.parse("2026-07-01T10:00:00Z");

    private Bien appartementValide() {
        return new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 3,
                new BigDecimal("55.00"), false,
                80000L, 5000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        );
    }

    // --- Nominaux ---

    @Test
    void bien_appartement_valide_construit() {
        Bien b = appartementValide();
        assertThat(b.typeBien()).isEqualTo(TypeBien.APPARTEMENT);
        assertThat(b.libelleCommercial()).isEqualTo("T3");
    }

    @Test
    void bien_maison_valide_construit() {
        Bien b = new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.MAISON,
                null, null, 4,
                new BigDecimal("100.00"), false,
                120000L, 0L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        );
        assertThat(b.libelleCommercial()).isEqualTo("Maison");
    }

    @Test
    void bien_chambre_colocation_valide_construit() {
        BienId parentId = BienId.nouveau();
        Bien b = new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.CHAMBRE_COLOCATION,
                parentId, "Chambre A", 1,
                new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, MAINTENANT
        );
        assertThat(b.libelleCommercial()).isEqualTo("Chambre en colocation — Chambre A");
        assertThat(b.bienParentId()).isEqualTo(parentId);
    }

    @Test
    void libelle_commercial_appartement_6_pieces_retourne_T6plus() {
        Bien b = new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 6,
                new BigDecimal("120.00"), false,
                200000L, 10000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        );
        assertThat(b.libelleCommercial()).isEqualTo("T6+");
    }

    @Test
    void libelle_commercial_appartement_7_pieces_retourne_T6plus() {
        Bien b = new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 7,
                new BigDecimal("150.00"), false,
                250000L, 12000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        );
        assertThat(b.libelleCommercial()).isEqualTo("T6+");
    }

    // --- I-1 ---

    @Test
    void i1_proprietaire_null_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), null, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    // --- I-2 ---

    @Test
    void i2_typeBien_null_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, null,
                null, null, 2,
                new BigDecimal("40.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    // --- I-3 ---

    @Test
    void i3_nb_pieces_zero_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 0,
                new BigDecimal("40.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nbPiecesPrincipales");
    }

    // --- I-4 ---

    @Test
    void i4_surface_nulle_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                null, false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void i4_surface_negative_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("-1.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("surfaceM2");
    }

    @Test
    void i4_surface_trop_de_decimales_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.123"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("décimales");
    }

    // --- I-5 ---

    @Test
    void i5_loyer_negatif_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.00"), false,
                -1L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loyerHorsChargesEnCentimes");
    }

    // --- I-6 ---

    @Test
    void i6_charges_negatives_levent_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.00"), false,
                60000L, -1L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chargesEnCentimes");
    }

    // --- I-7 ---

    @Test
    void i7_disponibleAPartirDu_null_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, null, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    // --- I-8 ---

    @Test
    void i8_adresse_null_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("40.00"), false,
                60000L, 3000L, ModaliteCharges.PROVISION,
                null, DISPO, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    // --- I-CHARGES-1 ---

    @Test
    void i_charges_meuble_doit_avoir_modalite_forfait() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("30.00"), true,
                50000L, 3000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modaliteCharges");
    }

    @Test
    void i_charges_non_meuble_doit_avoir_modalite_provision() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                null, null, 2,
                new BigDecimal("30.00"), false,
                50000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modaliteCharges");
    }

    // --- I-COLOC-1 ---

    @Test
    void i_coloc_chambre_sans_parentId_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.CHAMBRE_COLOCATION,
                null, "Chambre A", 1,
                new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void i_coloc_chambre_sans_libelle_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.CHAMBRE_COLOCATION,
                BienId.nouveau(), null, 1,
                new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("libelleChambre");
    }

    @Test
    void i_coloc_bien_non_chambre_avec_parentId_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.APPARTEMENT,
                BienId.nouveau(), null, 3,
                new BigDecimal("55.00"), false,
                80000L, 5000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bienParentId interdit");
    }

    @Test
    void i_coloc_bien_non_chambre_avec_libelle_leve_exception() {
        assertThatThrownBy(() -> new Bien(
                BienId.nouveau(), PROPRIETAIRE, TypeBien.MAISON,
                null, "Chambre B", 4,
                new BigDecimal("100.00"), false,
                120000L, 0L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, MAINTENANT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("libelleChambre interdit");
    }
}

package com.immo.gestion.bien.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.in.BienNonTrouveException;
import com.immo.gestion.bien.domain.port.in.BienParentIntrouvableException;
import com.immo.gestion.bien.domain.port.in.CreerBienCommand;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurParentException;
import com.immo.gestion.bien.domain.port.in.LibelleChambreNonUniqueException;
import com.immo.gestion.bien.domain.port.in.SurfaceChambresDepasseeException;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreerBienServiceTest {

    private static final Instant T = Instant.parse("2026-07-01T10:00:00Z");
    private static final Adresse ADRESSE = new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    private static final LocalDate DISPO = LocalDate.of(2026, 8, 1);

    private BienRepository repository;
    private BienQueryRepository queryRepository;
    private BienService service;
    private UtilisateurId proprietaireId;

    @BeforeEach
    void setUp() {
        repository = mock(BienRepository.class);
        queryRepository = mock(BienQueryRepository.class);
        Clock clock = Clock.fixed(T, ZoneOffset.UTC);
        service = new BienService(repository, queryRepository, clock);
        proprietaireId = UtilisateurId.nouveau();
    }

    private CreerBienCommand commandeAppartement() {
        return new CreerBienCommand(
                proprietaireId, TypeBien.APPARTEMENT, null, null,
                3, new BigDecimal("55.00"), false,
                80000L, 5000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO
        );
    }

    private Bien bienAvec(BienId id, UtilisateurId proprio, TypeBien type,
                          BienId parentId, String libelle, BigDecimal surface) {
        boolean meuble = type == TypeBien.CHAMBRE_COLOCATION;
        return new Bien(id, proprio, type, parentId, libelle, 1, surface,
                meuble, 45000L, 3000L,
                meuble ? ModaliteCharges.FORFAIT : ModaliteCharges.PROVISION,
                ADRESSE, DISPO, T);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DomainEvent>> eventsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    // --- Nominaux ---

    @Test
    void creer_appartement_persiste_et_publie_evenement() {
        FicheBien fiche = service.creer(commandeAppartement());

        ArgumentCaptor<BienId> idCaptor = ArgumentCaptor.forClass(BienId.class);
        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(repository).enregistrer(idCaptor.capture(), eq(0L), evenementsCaptor.capture());

        BienId bienId = idCaptor.getValue();
        assertThat(evenementsCaptor.getValue()).hasSize(1);
        BienAjouteAuPortefeuille evenement = (BienAjouteAuPortefeuille) evenementsCaptor.getValue().get(0);
        assertThat(evenement.bienId()).isEqualTo(bienId);
        assertThat(evenement.typeBien()).isEqualTo(TypeBien.APPARTEMENT);
        assertThat(evenement.proprietaireInitialId()).isEqualTo(proprietaireId);
        assertThat(evenement.survenuLe()).isEqualTo(T);

        assertThat(fiche.bienId()).isEqualTo(bienId);
        assertThat(fiche.libelleCommercial()).isEqualTo("T3");
    }

    @Test
    void obtenir_portefeuille_retourne_lignes_du_proprietaire() {
        BienId id1 = BienId.nouveau();
        BienId id2 = BienId.nouveau();
        Bien b1 = bienAvec(id1, proprietaireId, TypeBien.APPARTEMENT, null, null, new BigDecimal("55.00"));
        Bien b2 = bienAvec(id2, proprietaireId, TypeBien.MAISON, null, null, new BigDecimal("100.00"));
        when(queryRepository.chargerParProprietaire(proprietaireId)).thenReturn(List.of(b1, b2));

        List<LignePortefeuille> lignes = service.obtenir(proprietaireId);

        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).bienId()).isEqualTo(id1);
        assertThat(lignes.get(1).bienId()).isEqualTo(id2);
    }

    @Test
    void obtenir_fiche_retourne_bien_existant() {
        BienId id = BienId.nouveau();
        Bien b = bienAvec(id, proprietaireId, TypeBien.APPARTEMENT, null, null, new BigDecimal("55.00"));
        when(queryRepository.chargerParId(id)).thenReturn(Optional.of(b));

        FicheBien fiche = service.obtenir(id, proprietaireId);

        assertThat(fiche.bienId()).isEqualTo(id);
    }

    @Test
    void obtenir_fiche_bien_inexistant_leve_exception() {
        BienId id = BienId.nouveau();
        when(queryRepository.chargerParId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(id, proprietaireId))
                .isInstanceOf(BienNonTrouveException.class);
    }

    @Test
    void obtenir_fiche_bien_appartenant_a_un_autre_utilisateur_leve_exception() {
        BienId id = BienId.nouveau();
        UtilisateurId autreProprietaire = UtilisateurId.nouveau();
        Bien b = bienAvec(id, autreProprietaire, TypeBien.APPARTEMENT, null, null, new BigDecimal("55.00"));
        when(queryRepository.chargerParId(id)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.obtenir(id, proprietaireId))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    // --- Invariants colocation cross-aggregate ---

    @Test
    void creer_chambre_coloc_parent_introuvable_leve_exception() {
        BienId parentId = BienId.nouveau();
        when(queryRepository.chargerParId(parentId)).thenReturn(Optional.empty());

        CreerBienCommand cmd = new CreerBienCommand(
                proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A",
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO
        );

        assertThatThrownBy(() -> service.creer(cmd))
                .isInstanceOf(BienParentIntrouvableException.class);
    }

    @Test
    void creer_chambre_coloc_droit_insuffisant_sur_parent_leve_exception() {
        BienId parentId = BienId.nouveau();
        UtilisateurId autreProprietaire = UtilisateurId.nouveau();
        Bien parent = bienAvec(parentId, autreProprietaire, TypeBien.APPARTEMENT,
                null, null, new BigDecimal("80.00"));
        when(queryRepository.chargerParId(parentId)).thenReturn(Optional.of(parent));

        CreerBienCommand cmd = new CreerBienCommand(
                proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A",
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO
        );

        assertThatThrownBy(() -> service.creer(cmd))
                .isInstanceOf(DroitInsuffisantSurParentException.class);
    }

    @Test
    void creer_chambre_coloc_surface_depassee_leve_exception() {
        BienId parentId = BienId.nouveau();
        Bien parent = bienAvec(parentId, proprietaireId, TypeBien.APPARTEMENT,
                null, null, new BigDecimal("20.00"));
        when(queryRepository.chargerParId(parentId)).thenReturn(Optional.of(parent));
        // Chambre existante occupe déjà 15 m²
        Bien chambreExistante = bienAvec(BienId.nouveau(), proprietaireId,
                TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A", new BigDecimal("15.00"));
        when(queryRepository.chargerChambresParParent(parentId)).thenReturn(List.of(chambreExistante));

        CreerBienCommand cmd = new CreerBienCommand(
                proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre B",
                1, new BigDecimal("10.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO
        );

        assertThatThrownBy(() -> service.creer(cmd))
                .isInstanceOf(SurfaceChambresDepasseeException.class);
    }

    @Test
    void creer_chambre_coloc_libelle_existant_leve_exception() {
        BienId parentId = BienId.nouveau();
        Bien parent = bienAvec(parentId, proprietaireId, TypeBien.APPARTEMENT,
                null, null, new BigDecimal("80.00"));
        when(queryRepository.chargerParId(parentId)).thenReturn(Optional.of(parent));
        Bien chambreExistante = bienAvec(BienId.nouveau(), proprietaireId,
                TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A", new BigDecimal("12.00"));
        when(queryRepository.chargerChambresParParent(parentId)).thenReturn(List.of(chambreExistante));

        CreerBienCommand cmd = new CreerBienCommand(
                proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, "chambre a",
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO
        );

        assertThatThrownBy(() -> service.creer(cmd))
                .isInstanceOf(LibelleChambreNonUniqueException.class);
    }

    @Test
    void creer_chambre_coloc_valide_persiste_et_publie_evenement() {
        BienId parentId = BienId.nouveau();
        Bien parent = bienAvec(parentId, proprietaireId, TypeBien.APPARTEMENT,
                null, null, new BigDecimal("80.00"));
        when(queryRepository.chargerParId(parentId)).thenReturn(Optional.of(parent));
        when(queryRepository.chargerChambresParParent(parentId)).thenReturn(List.of());

        CreerBienCommand cmd = new CreerBienCommand(
                proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, "Chambre A",
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO
        );

        FicheBien fiche = service.creer(cmd);

        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(repository).enregistrer(any(BienId.class), eq(0L), evenementsCaptor.capture());
        assertThat(evenementsCaptor.getValue()).hasSize(1)
                .first().isInstanceOf(BienAjouteAuPortefeuille.class);
        assertThat(fiche.libelleCommercial()).isEqualTo("Chambre en colocation — Chambre A");
    }
}

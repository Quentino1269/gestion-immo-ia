package com.immo.gestion.bien.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.ChargesRevisees;
import com.immo.gestion.bien.domain.DisponibiliteRevisee;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.LibelleChambreRenomme;
import com.immo.gestion.bien.domain.LogementDevenuNu;
import com.immo.gestion.bien.domain.LoyerRevise;
import com.immo.gestion.bien.domain.MeubleEntreDansLeLogement;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.NombrePiecesRevise;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.in.BienNonTrouveException;
import com.immo.gestion.bien.domain.port.in.LibelleChambreNonUniqueException;
import com.immo.gestion.bien.domain.port.in.ModifierBienCommand;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModifierBienServiceTest {

    private static final Instant T_CREATION = Instant.parse("2026-07-01T10:00:00Z");
    private static final Instant T_MODIF = Instant.parse("2026-09-03T09:00:00Z");
    private static final Adresse ADRESSE = new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    private static final LocalDate DISPO = LocalDate.of(2026, 8, 1);

    private BienRepository repository;
    private BienQueryRepository queryRepository;
    private BienService service;
    private UtilisateurId proprietaireId;
    private BienId bienId;

    @BeforeEach
    void setUp() {
        repository = mock(BienRepository.class);
        queryRepository = mock(BienQueryRepository.class);
        Clock clock = Clock.fixed(T_MODIF, ZoneOffset.UTC);
        service = new BienService(repository, queryRepository, clock);
        proprietaireId = UtilisateurId.nouveau();
        bienId = BienId.nouveau();
    }

    private Bien appartementExistant() {
        return new Bien(bienId, proprietaireId, TypeBien.APPARTEMENT, null, null,
                3, new BigDecimal("55.00"), false,
                80000L, 5000L, ModaliteCharges.PROVISION,
                ADRESSE, DISPO, T_CREATION);
    }

    private Bien chambreExistante(BienId parentId, String libelle) {
        return new Bien(bienId, proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId, libelle,
                1, new BigDecimal("12.00"), true,
                45000L, 3000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, T_CREATION);
    }

    private ModifierBienCommand commandeIdentique(Bien existante) {
        return new ModifierBienCommand(
                bienId, proprietaireId,
                existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(), existante.meuble(),
                existante.disponibleAPartirDu(), existante.libelleChambre(), existante.nbPiecesPrincipales()
        );
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DomainEvent>> eventsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    // --- Refus ---

    @Test
    void modifier_bien_introuvable_leve_exception() {
        when(repository.chargerParId(bienId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(commandeIdentique(appartementExistant())))
                .isInstanceOf(BienNonTrouveException.class);
    }

    @Test
    void modifier_droit_insuffisant_leve_exception() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));
        UtilisateurId autre = UtilisateurId.nouveau();

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, autre, 85000L, existante.chargesEnCentimes(), existante.meuble(),
                existante.disponibleAPartirDu(), null, existante.nbPiecesPrincipales()
        );

        assertThatThrownBy(() -> service.modifier(cmd))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
        verify(repository, never()).enregistrer(any(), anyLong(), any());
    }

    @Test
    void modifier_nb_pieces_invalide_leve_exception() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                existante.meuble(), existante.disponibleAPartirDu(), null, 0
        );

        assertThatThrownBy(() -> service.modifier(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifier_libelle_chambre_deja_utilise_par_une_autre_chambre_leve_exception() {
        BienId parentId = BienId.nouveau();
        Bien existante = chambreExistante(parentId, "Chambre A");
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));
        Bien autreChambre = new Bien(BienId.nouveau(), proprietaireId, TypeBien.CHAMBRE_COLOCATION, parentId,
                "Chambre B", 1, new BigDecimal("10.00"), true, 40000L, 2000L, ModaliteCharges.FORFAIT,
                ADRESSE, DISPO, T_CREATION);
        when(queryRepository.chargerChambresParParent(parentId)).thenReturn(List.of(existante, autreChambre));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                existante.meuble(), existante.disponibleAPartirDu(), "chambre b", existante.nbPiecesPrincipales()
        );

        assertThatThrownBy(() -> service.modifier(cmd))
                .isInstanceOf(LibelleChambreNonUniqueException.class);
    }

    // --- No-op (D4 / I-MOD-8) ---

    @Test
    void modifier_sans_changement_est_un_noop() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        FicheBien fiche = service.modifier(commandeIdentique(existante));

        verify(repository, never()).enregistrer(any(), anyLong(), any());
        assertThat(fiche.loyerHorsChargesEnCentimes()).isEqualTo(existante.loyerHorsChargesEnCentimes());
    }

    @Test
    void modifier_meme_libelle_chambre_ne_declenche_pas_la_verification_unicite() {
        BienId parentId = BienId.nouveau();
        Bien existante = chambreExistante(parentId, "Chambre A");
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        FicheBien fiche = service.modifier(commandeIdentique(existante));

        verify(repository, never()).enregistrer(any(), anyLong(), any());
        assertThat(fiche.libelleChambre()).isEqualTo("Chambre A");
    }

    // --- Champs individuels (golden path) ---

    @Test
    void modifier_loyer_emet_loyer_revise() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, 90000L, existante.chargesEnCentimes(), existante.meuble(),
                existante.disponibleAPartirDu(), null, existante.nbPiecesPrincipales()
        );
        FicheBien fiche = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        LoyerRevise evenement = (LoyerRevise) captor.getValue().get(0);
        assertThat(evenement.loyerHorsChargesEnCentimes()).isEqualTo(90000L);
        assertThat(evenement.survenuLe()).isEqualTo(T_MODIF);
        assertThat(fiche.loyerHorsChargesEnCentimes()).isEqualTo(90000L);
    }

    @Test
    void modifier_charges_emet_charges_revisees() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), 6000L, existante.meuble(),
                existante.disponibleAPartirDu(), null, existante.nbPiecesPrincipales()
        );
        service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(ChargesRevisees.class);
        assertThat(((ChargesRevisees) captor.getValue().get(0)).chargesEnCentimes()).isEqualTo(6000L);
    }

    @Test
    void modifier_meuble_de_faux_a_vrai_emet_meuble_entre_et_derive_forfait() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                true, existante.disponibleAPartirDu(), null, existante.nbPiecesPrincipales()
        );
        FicheBien fiche = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        MeubleEntreDansLeLogement evenement = (MeubleEntreDansLeLogement) captor.getValue().get(0);
        assertThat(evenement.modaliteCharges()).isEqualTo(ModaliteCharges.FORFAIT);
        assertThat(fiche.meuble()).isTrue();
        assertThat(fiche.modaliteCharges()).isEqualTo(ModaliteCharges.FORFAIT);
    }

    @Test
    void modifier_meuble_de_vrai_a_faux_emet_logement_devenu_nu_et_derive_provision() {
        Bien existante = chambreExistante(BienId.nouveau(), "Chambre A");
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                false, existante.disponibleAPartirDu(), "Chambre A", existante.nbPiecesPrincipales()
        );
        FicheBien fiche = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        LogementDevenuNu evenement = (LogementDevenuNu) captor.getValue().get(0);
        assertThat(evenement.modaliteCharges()).isEqualTo(ModaliteCharges.PROVISION);
        assertThat(fiche.modaliteCharges()).isEqualTo(ModaliteCharges.PROVISION);
    }

    @Test
    void modifier_disponibilite_emet_disponibilite_revisee() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));
        LocalDate nouvelleDate = LocalDate.of(2026, 12, 1);

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                existante.meuble(), nouvelleDate, null, existante.nbPiecesPrincipales()
        );
        service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(DisponibiliteRevisee.class);
        assertThat(((DisponibiliteRevisee) captor.getValue().get(0)).disponibleAPartirDu()).isEqualTo(nouvelleDate);
    }

    @Test
    void modifier_libelle_chambre_unique_emet_libelle_chambre_renomme() {
        BienId parentId = BienId.nouveau();
        Bien existante = chambreExistante(parentId, "Chambre A");
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));
        when(queryRepository.chargerChambresParParent(parentId)).thenReturn(List.of(existante));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                existante.meuble(), existante.disponibleAPartirDu(), "Chambre côté cour", existante.nbPiecesPrincipales()
        );
        FicheBien fiche = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(LibelleChambreRenomme.class);
        assertThat(fiche.libelleChambre()).isEqualTo("Chambre côté cour");
    }

    @Test
    void modifier_nb_pieces_emet_nombre_pieces_revise() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, existante.loyerHorsChargesEnCentimes(), existante.chargesEnCentimes(),
                existante.meuble(), existante.disponibleAPartirDu(), null, 4
        );
        FicheBien fiche = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(NombrePiecesRevise.class);
        assertThat(fiche.libelleCommercial()).isEqualTo("T4");
    }

    @Test
    void modifier_plusieurs_champs_a_la_fois_emet_plusieurs_evenements() {
        Bien existante = appartementExistant();
        when(repository.chargerParId(bienId)).thenReturn(Optional.of(new EtatCharge<>(existante, 1)));

        ModifierBienCommand cmd = new ModifierBienCommand(
                bienId, proprietaireId, 90000L, 6000L, existante.meuble(),
                existante.disponibleAPartirDu(), null, existante.nbPiecesPrincipales()
        );
        service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> captor = eventsCaptor();
        verify(repository).enregistrer(eq(bienId), eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .anyMatch(LoyerRevise.class::isInstance)
                .anyMatch(ChargesRevisees.class::isInstance);
    }
}

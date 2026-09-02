package com.immo.gestion.rentabilite.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneComparateur;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.in.BienNonTrouveException;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteModifiee;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.LignesRevenuIncoherentesException;
import com.immo.gestion.rentabilite.domain.port.in.ModifierSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.RegimeFiscalIncoherentException;
import com.immo.gestion.rentabilite.domain.port.in.SimulationNonTrouveeException;
import com.immo.gestion.rentabilite.domain.port.in.TypeBienInvalidePourSimulationException;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteQueryRepository;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteRepository;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SimulationRentabiliteServiceTest {

    private static final Instant T = Instant.parse("2026-08-26T10:00:00Z");
    private static final Adresse ADRESSE = new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    private static final LocalDate DISPO = LocalDate.of(2026, 9, 1);

    private SimulationRentabiliteRepository repository;
    private SimulationRentabiliteQueryRepository queryRepository;
    private BienQueryRepository bienQueryRepository;
    private SimulationRentabiliteService service;
    private UtilisateurId proprietaireId;

    @BeforeEach
    void setUp() {
        repository = mock(SimulationRentabiliteRepository.class);
        queryRepository = mock(SimulationRentabiliteQueryRepository.class);
        bienQueryRepository = mock(BienQueryRepository.class);
        Clock clock = Clock.fixed(T, ZoneOffset.UTC);
        service = new SimulationRentabiliteService(repository, queryRepository, bienQueryRepository, clock);
        proprietaireId = UtilisateurId.nouveau();
    }

    private Bien bienAvec(BienId id, UtilisateurId proprio, TypeBien type, boolean meuble, BienId parentId) {
        return new Bien(id, proprio, type, parentId, type == TypeBien.CHAMBRE_COLOCATION ? "Chambre A" : null, 3,
                new BigDecimal("55.00"), meuble, 80_000L, 5_000L,
                meuble ? ModaliteCharges.FORFAIT : ModaliteCharges.PROVISION,
                ADRESSE, DISPO, T);
    }

    private LancerSimulationRentabiliteCommand commande(BienId bienId, RegimeFiscal regime, List<LigneRevenuSimule> revenus) {
        return new LancerSimulationRentabiliteCommand(
                bienId, proprietaireId, "Scénario test", regime, 30, 1,
                new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L),
                new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO),
                new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7),
                revenus,
                new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L),
                new HypothesesEvolution(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DomainEvent>> eventsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    // --- Golden path ---

    @Test
    void lancer_simulation_bien_sans_coloc_persiste_et_publie_evenement() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(bienId)).thenReturn(List.of());

        LancerSimulationRentabiliteCommand cmd = commande(
                bienId, RegimeFiscal.MICRO_FONCIER, List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)));

        SimulationRentabilite resultat = service.lancer(cmd);

        ArgumentCaptor<SimulationRentabiliteId> idCaptor = ArgumentCaptor.forClass(SimulationRentabiliteId.class);
        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(repository).enregistrer(idCaptor.capture(), eq(0L), evenementsCaptor.capture());

        assertThat(evenementsCaptor.getValue()).hasSize(1);
        RentabiliteSimulee evenement = (RentabiliteSimulee) evenementsCaptor.getValue().get(0);
        assertThat(evenement.simulationId()).isEqualTo(idCaptor.getValue());
        assertThat(evenement.bienId()).isEqualTo(bienId);
        assertThat(evenement.survenuLe()).isEqualTo(T);
        assertThat(resultat.id()).isEqualTo(idCaptor.getValue());
        assertThat(resultat.projectionAnnuelle()).hasSize(1);
    }

    @Test
    void lancer_simulation_coloc_agrege_les_chambres_actives() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.MAISON, false, null);
        BienId chambre1 = BienId.nouveau();
        BienId chambre2 = BienId.nouveau();
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(bienId)).thenReturn(List.of(
                bienAvec(chambre1, proprietaireId, TypeBien.CHAMBRE_COLOCATION, true, bienId),
                bienAvec(chambre2, proprietaireId, TypeBien.CHAMBRE_COLOCATION, true, bienId)
        ));

        LancerSimulationRentabiliteCommand cmd = commande(bienId, RegimeFiscal.MICRO_FONCIER, List.of(
                new LigneRevenuSimule(chambre1, 60_000L, 0L),
                new LigneRevenuSimule(chambre2, 40_000L, 0L)
        ));

        SimulationRentabilite resultat = service.lancer(cmd);

        assertThat(resultat.projectionAnnuelle().get(0).loyerBrutAnnuelEnCentimes()).isEqualTo(1_200_000L);
    }

    @Test
    void lancer_simulation_bien_introuvable_leve_exception() {
        BienId bienId = BienId.nouveau();
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lancer(commande(bienId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)))))
                .isInstanceOf(BienNonTrouveException.class);
        verifyNoInteractions(repository);
    }

    // --- I-SIM-1 ---

    @Test
    void lancer_simulation_sur_une_chambre_leve_exception() {
        BienId parentId = BienId.nouveau();
        BienId chambreId = BienId.nouveau();
        Bien chambre = bienAvec(chambreId, proprietaireId, TypeBien.CHAMBRE_COLOCATION, true, parentId);
        when(bienQueryRepository.chargerParId(chambreId)).thenReturn(Optional.of(chambre));

        assertThatThrownBy(() -> service.lancer(commande(chambreId, RegimeFiscal.MICRO_BIC,
                List.of(new LigneRevenuSimule(chambreId, 100_000L, 0L)))))
                .isInstanceOf(TypeBienInvalidePourSimulationException.class);
    }

    // --- I-SIM-2 ---

    @Test
    void lancer_simulation_utilisateur_non_ayant_droit_leve_exception() {
        BienId bienId = BienId.nouveau();
        UtilisateurId autreProprietaire = UtilisateurId.nouveau();
        Bien bien = bienAvec(bienId, autreProprietaire, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));

        assertThatThrownBy(() -> service.lancer(commande(bienId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)))))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    // --- I-SIM-3 ---

    @Test
    void lancer_simulation_regime_meuble_sur_bien_nu_leve_exception() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(bienId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.lancer(commande(bienId, RegimeFiscal.MICRO_BIC,
                List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)))))
                .isInstanceOf(RegimeFiscalIncoherentException.class);
    }

    // --- I-SIM-11 ---

    @Test
    void lancer_simulation_ligne_pour_bien_etranger_leve_exception() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(bienId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.lancer(commande(bienId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(BienId.nouveau(), 100_000L, 0L)))))
                .isInstanceOf(LignesRevenuIncoherentesException.class);
    }

    @Test
    void lancer_simulation_coloc_chambre_manquante_leve_exception() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.MAISON, false, null);
        BienId chambre1 = BienId.nouveau();
        BienId chambre2 = BienId.nouveau();
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(bienId)).thenReturn(List.of(
                bienAvec(chambre1, proprietaireId, TypeBien.CHAMBRE_COLOCATION, true, bienId),
                bienAvec(chambre2, proprietaireId, TypeBien.CHAMBRE_COLOCATION, true, bienId)
        ));

        // Une seule ligne fournie alors que 2 chambres actives existent.
        assertThatThrownBy(() -> service.lancer(commande(bienId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(chambre1, 60_000L, 0L)))))
                .isInstanceOf(LignesRevenuIncoherentesException.class);
    }

    // --- obtenir(id, demandeurId) ---

    @Test
    void obtenir_simulation_retourne_la_simulation_du_demandeur() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite simulation = simulationAvec(id, proprietaireId);
        when(queryRepository.chargerParId(id)).thenReturn(Optional.of(simulation));

        assertThat(service.obtenir(id, proprietaireId)).isEqualTo(simulation);
    }

    @Test
    void obtenir_simulation_introuvable_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        when(queryRepository.chargerParId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(id, proprietaireId))
                .isInstanceOf(SimulationNonTrouveeException.class);
    }

    @Test
    void obtenir_simulation_d_un_autre_utilisateur_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite simulation = simulationAvec(id, UtilisateurId.nouveau());
        when(queryRepository.chargerParId(id)).thenReturn(Optional.of(simulation));

        assertThatThrownBy(() -> service.obtenir(id, proprietaireId))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    // --- obtenirComparateur(bienId, demandeurId) ---

    @Test
    void obtenir_comparateur_retourne_les_scenarios_du_bien() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));
        SimulationRentabilite s1 = simulationAvec(SimulationRentabiliteId.nouveau(), proprietaireId);
        when(queryRepository.chargerParBien(bienId)).thenReturn(List.of(s1));

        List<LigneComparateur> lignes = service.obtenir(bienId, proprietaireId);

        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0).simulationId()).isEqualTo(s1.id());
    }

    @Test
    void obtenir_comparateur_bien_introuvable_leve_exception() {
        BienId bienId = BienId.nouveau();
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(bienId, proprietaireId))
                .isInstanceOf(BienNonTrouveException.class);
    }

    @Test
    void obtenir_comparateur_utilisateur_non_ayant_droit_leve_exception() {
        BienId bienId = BienId.nouveau();
        Bien bien = bienAvec(bienId, UtilisateurId.nouveau(), TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(bienId)).thenReturn(Optional.of(bien));

        assertThatThrownBy(() -> service.obtenir(bienId, proprietaireId))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    // --- modifier(commande) ---

    private ModifierSimulationRentabiliteCommand commandeModification(
            SimulationRentabiliteId id, UtilisateurId utilisateurId, RegimeFiscal regime, List<LigneRevenuSimule> revenus
    ) {
        return new ModifierSimulationRentabiliteCommand(
                id, utilisateurId, "Scénario modifié", regime, 30, 1,
                new ParametresAcquisition(25_000_000L, 0L, 0L, 0L, 0L),
                new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO),
                new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7),
                revenus,
                new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L),
                new HypothesesEvolution(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    @Test
    void modifier_simulation_reussie_persiste_un_nouvel_evenement_a_la_version_courante() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite existante = simulationAvec(id, proprietaireId);
        when(repository.chargerParId(id)).thenReturn(Optional.of(new EtatCharge<>(existante, 1L)));
        Bien bien = bienAvec(existante.bienId(), proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(existante.bienId())).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(existante.bienId())).thenReturn(List.of());

        ModifierSimulationRentabiliteCommand cmd = commandeModification(
                id, proprietaireId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(existante.bienId(), 120_000L, 0L)));

        SimulationRentabilite resultat = service.modifier(cmd);

        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(repository).enregistrer(eq(id), eq(1L), evenementsCaptor.capture());
        assertThat(evenementsCaptor.getValue()).hasSize(1);
        SimulationRentabiliteModifiee evenement = (SimulationRentabiliteModifiee) evenementsCaptor.getValue().get(0);
        assertThat(evenement.simulationId()).isEqualTo(id);
        assertThat(evenement.nomScenario()).isEqualTo("Scénario modifié");
        assertThat(resultat.id()).isEqualTo(id);
        assertThat(resultat.bienId()).isEqualTo(existante.bienId());
        assertThat(resultat.utilisateurId()).isEqualTo(proprietaireId);
        assertThat(resultat.acquisition().prixAchatEnCentimes()).isEqualTo(25_000_000L);
    }

    @Test
    void modifier_simulation_introuvable_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        when(repository.chargerParId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(commandeModification(
                id, proprietaireId, RegimeFiscal.MICRO_FONCIER, List.of(new LigneRevenuSimule(BienId.nouveau(), 100_000L, 0L)))))
                .isInstanceOf(SimulationNonTrouveeException.class);
    }

    @Test
    void modifier_simulation_d_un_autre_utilisateur_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite existante = simulationAvec(id, UtilisateurId.nouveau());
        when(repository.chargerParId(id)).thenReturn(Optional.of(new EtatCharge<>(existante, 1L)));

        assertThatThrownBy(() -> service.modifier(commandeModification(
                id, proprietaireId, RegimeFiscal.MICRO_FONCIER,
                List.of(new LigneRevenuSimule(existante.bienId(), 100_000L, 0L)))))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    @Test
    void modifier_simulation_regime_meuble_incoherent_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite existante = simulationAvec(id, proprietaireId);
        when(repository.chargerParId(id)).thenReturn(Optional.of(new EtatCharge<>(existante, 1L)));
        Bien bien = bienAvec(existante.bienId(), proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(existante.bienId())).thenReturn(Optional.of(bien));

        assertThatThrownBy(() -> service.modifier(commandeModification(
                id, proprietaireId, RegimeFiscal.MICRO_BIC,
                List.of(new LigneRevenuSimule(existante.bienId(), 100_000L, 0L)))))
                .isInstanceOf(RegimeFiscalIncoherentException.class);
    }

    @Test
    void modifier_simulation_ligne_revenu_incoherente_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite existante = simulationAvec(id, proprietaireId);
        when(repository.chargerParId(id)).thenReturn(Optional.of(new EtatCharge<>(existante, 1L)));
        Bien bien = bienAvec(existante.bienId(), proprietaireId, TypeBien.APPARTEMENT, false, null);
        when(bienQueryRepository.chargerParId(existante.bienId())).thenReturn(Optional.of(bien));
        when(bienQueryRepository.chargerChambresParParent(existante.bienId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.modifier(commandeModification(
                id, proprietaireId, RegimeFiscal.MICRO_FONCIER, List.of(new LigneRevenuSimule(BienId.nouveau(), 100_000L, 0L)))))
                .isInstanceOf(LignesRevenuIncoherentesException.class);
    }

    // --- obtenirHistorique(id, demandeurId) ---

    @Test
    void obtenir_historique_retourne_toutes_les_versions_du_demandeur() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite v1 = simulationAvec(id, proprietaireId);
        SimulationRentabilite v2 = simulationAvec(id, proprietaireId);
        when(repository.chargerHistorique(id)).thenReturn(List.of(v1, v2));

        List<SimulationRentabilite> historique = service.obtenirHistorique(id, proprietaireId);

        assertThat(historique).containsExactly(v1, v2);
    }

    @Test
    void obtenir_historique_introuvable_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        when(repository.chargerHistorique(id)).thenReturn(List.of());

        assertThatThrownBy(() -> service.obtenirHistorique(id, proprietaireId))
                .isInstanceOf(SimulationNonTrouveeException.class);
    }

    @Test
    void obtenir_historique_d_un_autre_utilisateur_leve_exception() {
        SimulationRentabiliteId id = SimulationRentabiliteId.nouveau();
        SimulationRentabilite v1 = simulationAvec(id, UtilisateurId.nouveau());
        when(repository.chargerHistorique(id)).thenReturn(List.of(v1));

        assertThatThrownBy(() -> service.obtenirHistorique(id, proprietaireId))
                .isInstanceOf(DroitInsuffisantSurBienException.class);
    }

    // --- Helper ---

    private SimulationRentabilite simulationAvec(SimulationRentabiliteId id, UtilisateurId utilisateurId) {
        BienId bienId = BienId.nouveau();
        ParametresAcquisition acquisition = new ParametresAcquisition(20_000_000L, 0L, 0L, 0L, 0L);
        return new SimulationRentabilite(
                id, bienId, utilisateurId, "Scénario test", RegimeFiscal.MICRO_FONCIER, 30, 1,
                acquisition,
                new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO),
                new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7),
                List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)),
                new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L),
                new HypothesesEvolution(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                acquisition.coutTotalEnCentimes(), acquisition.coutTotalEnCentimes(),
                com.immo.gestion.rentabilite.domain.ProjectionCalculateur.calculer(
                        RegimeFiscal.MICRO_FONCIER, 30, 1, acquisition,
                        new ParametresFinancement(0L, BigDecimal.ZERO, 0, BigDecimal.ZERO),
                        new ParametresAmortissement(new BigDecimal("15"), new BigDecimal("5"), 25, 7),
                        List.of(new LigneRevenuSimule(bienId, 100_000L, 0L)),
                        new ParametresChargesRecurrentes(0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L),
                        new HypothesesEvolution(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                ),
                T
        );
    }
}

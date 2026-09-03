package com.immo.gestion.rentabilite.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneComparateur;
import com.immo.gestion.rentabilite.domain.LigneProjection;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.ProjectionCalculateur;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteModifiee;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteSupprimee;
import com.immo.gestion.rentabilite.domain.port.in.BienNonTrouveException;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteUseCase;
import com.immo.gestion.rentabilite.domain.port.in.LignesRevenuIncoherentesException;
import com.immo.gestion.rentabilite.domain.port.in.ModifierSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.ModifierSimulationRentabiliteUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirComparateurUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirHistoriqueSimulationUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirSimulationUseCase;
import com.immo.gestion.rentabilite.domain.port.in.RegimeFiscalIncoherentException;
import com.immo.gestion.rentabilite.domain.port.in.SimulationNonTrouveeException;
import com.immo.gestion.rentabilite.domain.port.in.SimulationSupprimeeException;
import com.immo.gestion.rentabilite.domain.port.in.SupprimerSimulationCommand;
import com.immo.gestion.rentabilite.domain.port.in.SupprimerSimulationUseCase;
import com.immo.gestion.rentabilite.domain.port.in.TypeBienInvalidePourSimulationException;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteQueryRepository;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SimulationRentabiliteService
        implements LancerSimulationRentabiliteUseCase, ObtenirSimulationUseCase, ObtenirComparateurUseCase,
                   ModifierSimulationRentabiliteUseCase, ObtenirHistoriqueSimulationUseCase,
                   SupprimerSimulationUseCase {

    private final SimulationRentabiliteRepository repository;
    private final SimulationRentabiliteQueryRepository queryRepository;
    private final BienQueryRepository bienQueryRepository;
    private final Clock clock;

    public SimulationRentabiliteService(
            SimulationRentabiliteRepository repository,
            SimulationRentabiliteQueryRepository queryRepository,
            BienQueryRepository bienQueryRepository,
            Clock clock
    ) {
        this.repository = repository;
        this.queryRepository = queryRepository;
        this.bienQueryRepository = bienQueryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SimulationRentabilite lancer(LancerSimulationRentabiliteCommand commande) {
        Bien bienRacine = bienQueryRepository.chargerParId(commande.bienId())
                .orElseThrow(() -> new BienNonTrouveException(commande.bienId()));

        // I-SIM-1
        if (bienRacine.typeBien() != TypeBien.MAISON && bienRacine.typeBien() != TypeBien.APPARTEMENT) {
            throw new TypeBienInvalidePourSimulationException();
        }
        // I-SIM-2 (V1 mono-propriétaire, D15)
        if (!bienRacine.proprietaireInitialId().equals(commande.utilisateurId())) {
            throw new DroitInsuffisantSurBienException();
        }
        // I-SIM-3
        if (!commande.regimeFiscal().compatibleAvecMeuble(bienRacine.meuble())) {
            throw new RegimeFiscalIncoherentException();
        }

        List<Bien> chambresActives = bienQueryRepository.chargerChambresParParent(commande.bienId());
        verifierLignesRevenu(commande.bienId(), chambresActives, commande.revenusLocatifsSimules());

        SimulationRentabilite simulation = construire(
                SimulationRentabiliteId.nouveau(),
                commande.bienId(),
                commande.utilisateurId(),
                commande.nomScenario(),
                commande.regimeFiscal(),
                commande.tmiFoyerPourcent(),
                commande.horizonAnnees(),
                commande.acquisition(),
                commande.financement(),
                commande.amortissement(),
                commande.revenusLocatifsSimules(),
                commande.chargesRecurrentes(),
                commande.hypothesesEvolution()
        );

        DomainEvent evenement = RentabiliteSimulee.depuis(simulation);
        repository.enregistrer(simulation.id(), 0L, List.of(evenement));

        return simulation;
    }

    @Override
    @Transactional
    public SimulationRentabilite modifier(ModifierSimulationRentabiliteCommand commande) {
        EtatCharge<SimulationRentabilite> etatCharge = repository.chargerParId(commande.simulationId())
                .orElseThrow(() -> new SimulationNonTrouveeException(commande.simulationId()));
        SimulationRentabilite existante = etatCharge.aggregat();

        // Le lanceur (et donc le modificateur légitime) d'une simulation est, par construction
        // (I-SIM-2), l'ayant droit du bien au moment du calcul initial.
        if (!existante.utilisateurId().equals(commande.utilisateurId())) {
            throw new DroitInsuffisantSurBienException();
        }
        // I-SUPPR-4 : une simulation supprimée refuse toute modification.
        garantirNonSupprimee(existante);

        Bien bienRacine = bienQueryRepository.chargerParId(existante.bienId())
                .orElseThrow(() -> new BienNonTrouveException(existante.bienId()));
        // I-SIM-3, réévalué : le régime fiscal peut changer lors d'une modification.
        if (!commande.regimeFiscal().compatibleAvecMeuble(bienRacine.meuble())) {
            throw new RegimeFiscalIncoherentException();
        }

        List<Bien> chambresActives = bienQueryRepository.chargerChambresParParent(existante.bienId());
        verifierLignesRevenu(existante.bienId(), chambresActives, commande.revenusLocatifsSimules());

        SimulationRentabilite miseAJour = construire(
                existante.id(),
                existante.bienId(),
                existante.utilisateurId(),
                commande.nomScenario(),
                commande.regimeFiscal(),
                commande.tmiFoyerPourcent(),
                commande.horizonAnnees(),
                commande.acquisition(),
                commande.financement(),
                commande.amortissement(),
                commande.revenusLocatifsSimules(),
                commande.chargesRecurrentes(),
                commande.hypothesesEvolution()
        );

        // Append-only (D2 revisité) : ce fait s'ajoute au flux existant, il ne le remplace pas —
        // toutes les versions antérieures restent rejouables via l'historique.
        DomainEvent evenement = SimulationRentabiliteModifiee.depuis(miseAJour);
        repository.enregistrer(existante.id(), etatCharge.version(), List.of(evenement));

        return miseAJour;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulationRentabilite> obtenirHistorique(SimulationRentabiliteId id, UtilisateurId demandeurId) {
        List<SimulationRentabilite> historique = repository.chargerHistorique(id);
        if (historique.isEmpty()) {
            throw new SimulationNonTrouveeException(id);
        }
        if (!historique.get(0).utilisateurId().equals(demandeurId)) {
            throw new DroitInsuffisantSurBienException();
        }
        // I-SUPPR-5 (D2) : une simulation supprimée se comporte comme si elle n'existait plus.
        if (historique.get(historique.size() - 1).supprimee()) {
            throw new SimulationNonTrouveeException(id);
        }
        return historique;
    }

    /** I-SUPPR-4/I-SUPPR-5 : une simulation supprimée refuse toute écriture ultérieure. */
    private void garantirNonSupprimee(SimulationRentabilite simulation) {
        if (simulation.supprimee()) {
            throw new SimulationSupprimeeException(simulation.id());
        }
    }

    @Override
    @Transactional
    public void supprimer(SupprimerSimulationCommand commande) {
        EtatCharge<SimulationRentabilite> etatCharge = repository.chargerParId(commande.simulationId())
                .orElseThrow(() -> new SimulationNonTrouveeException(commande.simulationId()));
        SimulationRentabilite existante = etatCharge.aggregat();

        if (!existante.utilisateurId().equals(commande.demandeurId())) {
            throw new DroitInsuffisantSurBienException();
        }
        // I-SUPPR-3 (D4) : no-op idempotent si déjà supprimée.
        if (existante.supprimee()) {
            return;
        }

        DomainEvent evenement = new SimulationRentabiliteSupprimee(existante.id(), Instant.now(clock));
        repository.enregistrer(existante.id(), etatCharge.version(), List.of(evenement));
    }

    @Override
    @Transactional(readOnly = true)
    public SimulationRentabilite obtenir(SimulationRentabiliteId id, UtilisateurId demandeurId) {
        SimulationRentabilite simulation = queryRepository.chargerParId(id)
                .orElseThrow(() -> new SimulationNonTrouveeException(id));
        // Le lanceur d'une simulation est, par construction (I-SIM-2), l'ayant droit du bien au moment du calcul.
        if (!simulation.utilisateurId().equals(demandeurId)) {
            throw new DroitInsuffisantSurBienException();
        }
        return simulation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LigneComparateur> obtenir(BienId bienId, UtilisateurId demandeurId) {
        Bien bien = bienQueryRepository.chargerParId(bienId)
                .orElseThrow(() -> new BienNonTrouveException(bienId));
        if (!bien.proprietaireInitialId().equals(demandeurId)) {
            throw new DroitInsuffisantSurBienException();
        }
        return queryRepository.chargerParBien(bienId).stream()
                .map(LigneComparateur::depuis)
                .toList();
    }

    private SimulationRentabilite construire(
            SimulationRentabiliteId id,
            BienId bienId,
            UtilisateurId utilisateurId,
            String nomScenario,
            RegimeFiscal regimeFiscal,
            int tmiFoyerPourcent,
            int horizonAnnees,
            ParametresAcquisition acquisition,
            ParametresFinancement financement,
            ParametresAmortissement amortissement,
            List<LigneRevenuSimule> revenusLocatifsSimules,
            ParametresChargesRecurrentes chargesRecurrentes,
            HypothesesEvolution hypothesesEvolution
    ) {
        long coutTotal = acquisition.coutTotalEnCentimes();
        long apport = coutTotal - financement.montantEmprunteEnCentimes();

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
                regimeFiscal,
                tmiFoyerPourcent,
                horizonAnnees,
                acquisition,
                financement,
                amortissement,
                revenusLocatifsSimules,
                chargesRecurrentes,
                hypothesesEvolution
        );

        return new SimulationRentabilite(
                id,
                bienId,
                utilisateurId,
                nomScenario,
                regimeFiscal,
                tmiFoyerPourcent,
                horizonAnnees,
                acquisition,
                financement,
                amortissement,
                revenusLocatifsSimules,
                chargesRecurrentes,
                hypothesesEvolution,
                coutTotal,
                apport,
                projection,
                Instant.now(clock),
                false
        );
    }

    // --- I-SIM-11 ---

    private void verifierLignesRevenu(BienId bienRacineId, List<Bien> chambresActives, List<LigneRevenuSimule> lignes) {
        Set<UUID> idsAttendus = chambresActives.isEmpty()
                ? Set.of(bienRacineId.valeur())
                : chambresActives.stream().map(c -> c.id().valeur()).collect(Collectors.toSet());

        List<UUID> idsFournis = lignes.stream().map(l -> l.bienSourceId().valeur()).toList();
        Set<UUID> idsFournisUniques = Set.copyOf(idsFournis);
        if (idsFournis.size() != idsFournisUniques.size()) {
            throw new LignesRevenuIncoherentesException("doublons détectés parmi les lignes de revenu");
        }
        if (!idsFournisUniques.equals(idsAttendus)) {
            throw new LignesRevenuIncoherentesException(chambresActives.isEmpty()
                    ? "attendu une unique ligne pour le bien racine lui-même (aucune chambre active)"
                    : "attendu exactement une ligne par chambre active du bien, sans omission ni bien étranger");
        }
    }
}

package com.immo.gestion.rentabilite.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.rentabilite.domain.LigneComparateur;
import com.immo.gestion.rentabilite.domain.LigneProjection;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ProjectionCalculateur;
import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.in.BienNonTrouveException;
import com.immo.gestion.rentabilite.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteUseCase;
import com.immo.gestion.rentabilite.domain.port.in.LignesRevenuIncoherentesException;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirComparateurUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirSimulationUseCase;
import com.immo.gestion.rentabilite.domain.port.in.RegimeFiscalIncoherentException;
import com.immo.gestion.rentabilite.domain.port.in.SimulationNonTrouveeException;
import com.immo.gestion.rentabilite.domain.port.in.TypeBienInvalidePourSimulationException;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteQueryRepository;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteRepository;
import com.immo.gestion.shared.domain.DomainEvent;
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
        implements LancerSimulationRentabiliteUseCase, ObtenirSimulationUseCase, ObtenirComparateurUseCase {

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

        long coutTotal = commande.acquisition().coutTotalEnCentimes();
        long apport = coutTotal - commande.financement().montantEmprunteEnCentimes();

        List<LigneProjection> projection = ProjectionCalculateur.calculer(
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

        SimulationRentabilite simulation = new SimulationRentabilite(
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
                commande.hypothesesEvolution(),
                coutTotal,
                apport,
                projection,
                Instant.now(clock)
        );

        DomainEvent evenement = RentabiliteSimulee.depuis(simulation);
        repository.enregistrer(simulation.id(), 0L, List.of(evenement));

        return simulation;
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

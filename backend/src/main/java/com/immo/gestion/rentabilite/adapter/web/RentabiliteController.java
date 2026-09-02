package com.immo.gestion.rentabilite.adapter.web;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.LancerSimulationRentabiliteUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ModifierSimulationRentabiliteCommand;
import com.immo.gestion.rentabilite.domain.port.in.ModifierSimulationRentabiliteUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirComparateurUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirHistoriqueSimulationUseCase;
import com.immo.gestion.rentabilite.domain.port.in.ObtenirSimulationUseCase;
import com.immo.gestion.session.adapter.web.ContexteAuthentificationRequete;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RentabiliteController {

    private final LancerSimulationRentabiliteUseCase lancer;
    private final ObtenirSimulationUseCase obtenirSimulation;
    private final ObtenirComparateurUseCase obtenirComparateur;
    private final ModifierSimulationRentabiliteUseCase modifier;
    private final ObtenirHistoriqueSimulationUseCase obtenirHistorique;

    public RentabiliteController(
            LancerSimulationRentabiliteUseCase lancer,
            ObtenirSimulationUseCase obtenirSimulation,
            ObtenirComparateurUseCase obtenirComparateur,
            ModifierSimulationRentabiliteUseCase modifier,
            ObtenirHistoriqueSimulationUseCase obtenirHistorique
    ) {
        this.lancer = lancer;
        this.obtenirSimulation = obtenirSimulation;
        this.obtenirComparateur = obtenirComparateur;
        this.modifier = modifier;
        this.obtenirHistorique = obtenirHistorique;
    }

    @PostMapping("/biens/{bienId}/simulations-rentabilite")
    public ResponseEntity<SimulationRentabiliteResponse> lancerSimulation(
            @PathVariable UUID bienId,
            @RequestBody LancerSimulationRentabiliteRequest body,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SimulationRentabilite simulation = lancer.lancer(versCommande(new BienId(bienId), uid, body));

        URI emplacement = UriComponentsBuilder.fromPath("/api/simulations-rentabilite/{id}")
                .buildAndExpand(simulation.id().valeur())
                .toUri();
        return ResponseEntity.created(emplacement).body(SimulationRentabiliteResponse.depuis(simulation));
    }

    @GetMapping("/biens/{bienId}/simulations-rentabilite")
    public ResponseEntity<List<LigneComparateurResponse>> obtenirComparateur(
            @PathVariable UUID bienId,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<LigneComparateurResponse> lignes = obtenirComparateur.obtenir(new BienId(bienId), uid).stream()
                .map(LigneComparateurResponse::depuis)
                .toList();
        return ResponseEntity.ok(lignes);
    }

    @GetMapping("/simulations-rentabilite/{id}")
    public ResponseEntity<SimulationRentabiliteResponse> obtenirDetail(
            @PathVariable UUID id,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SimulationRentabilite simulation = obtenirSimulation.obtenir(new SimulationRentabiliteId(id), uid);
        return ResponseEntity.ok(SimulationRentabiliteResponse.depuis(simulation));
    }

    @PutMapping("/simulations-rentabilite/{id}")
    public ResponseEntity<SimulationRentabiliteResponse> modifierSimulation(
            @PathVariable UUID id,
            @RequestBody LancerSimulationRentabiliteRequest body,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SimulationRentabilite simulation = modifier.modifier(
                versCommandeModification(new SimulationRentabiliteId(id), uid, body));
        return ResponseEntity.ok(SimulationRentabiliteResponse.depuis(simulation));
    }

    @GetMapping("/simulations-rentabilite/{id}/historique")
    public ResponseEntity<List<SimulationRentabiliteResponse>> historiqueSimulation(
            @PathVariable UUID id,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<SimulationRentabiliteResponse> versions = obtenirHistorique
                .obtenirHistorique(new SimulationRentabiliteId(id), uid).stream()
                .map(SimulationRentabiliteResponse::depuis)
                .toList();
        return ResponseEntity.ok(versions);
    }

    private ModifierSimulationRentabiliteCommand versCommandeModification(
            SimulationRentabiliteId simulationId, UtilisateurId uid, LancerSimulationRentabiliteRequest r
    ) {
        List<LigneRevenuSimule> revenus = r.revenusLocatifsSimules().stream()
                .map(l -> new LigneRevenuSimule(
                        new BienId(l.bienSourceId()),
                        l.loyerSimuleMensuelEnCentimes(),
                        l.chargesSimuleesMensuellesEnCentimes()
                ))
                .toList();

        return new ModifierSimulationRentabiliteCommand(
                simulationId,
                uid,
                r.nomScenario(),
                r.regimeFiscal(),
                r.tmiFoyerPourcent(),
                r.horizonAnnees(),
                new ParametresAcquisition(
                        r.acquisition().prixAchatEnCentimes(),
                        r.acquisition().fraisNotaireEnCentimes(),
                        r.acquisition().fraisAgenceEnCentimes(),
                        r.acquisition().travauxAlAcquisitionEnCentimes(),
                        r.acquisition().fraisDossierBancaireEnCentimes()
                ),
                new ParametresFinancement(
                        r.financement().montantEmprunteEnCentimes(),
                        r.financement().tauxAnnuelPourcent(),
                        r.financement().dureeAnnees(),
                        r.financement().tauxAssuranceEmprunteurPourcent()
                ),
                new ParametresAmortissement(
                        r.amortissement().quotePartTerrainPourcent(),
                        r.amortissement().quotePartMobilierPourcent(),
                        r.amortissement().dureeAmortissementBatiAnnees(),
                        r.amortissement().dureeAmortissementMobilierAnnees()
                ),
                revenus,
                new ParametresChargesRecurrentes(
                        r.chargesRecurrentes().taxeFonciereEnCentimes(),
                        r.chargesRecurrentes().assurancePnoEnCentimes(),
                        r.chargesRecurrentes().assuranceLoyersImpayesEnCentimes(),
                        r.chargesRecurrentes().fraisGestionLocativePourcentLoyer(),
                        r.chargesRecurrentes().provisionTravauxAnnuelleEnCentimes(),
                        r.chargesRecurrentes().fraisComptabiliteAnnuelEnCentimes(),
                        r.chargesRecurrentes().chargesCoproprieteNonRecuperablesEnCentimes()
                ),
                new HypothesesEvolution(
                        r.hypothesesEvolution().tauxVacanceLocativePourcent(),
                        r.hypothesesEvolution().tauxIndexationLoyerPourcent(),
                        r.hypothesesEvolution().tauxIndexationChargesPourcent()
                )
        );
    }

    private LancerSimulationRentabiliteCommand versCommande(BienId bienId, UtilisateurId uid, LancerSimulationRentabiliteRequest r) {
        List<LigneRevenuSimule> revenus = r.revenusLocatifsSimules().stream()
                .map(l -> new LigneRevenuSimule(
                        new BienId(l.bienSourceId()),
                        l.loyerSimuleMensuelEnCentimes(),
                        l.chargesSimuleesMensuellesEnCentimes()
                ))
                .toList();

        return new LancerSimulationRentabiliteCommand(
                bienId,
                uid,
                r.nomScenario(),
                r.regimeFiscal(),
                r.tmiFoyerPourcent(),
                r.horizonAnnees(),
                new ParametresAcquisition(
                        r.acquisition().prixAchatEnCentimes(),
                        r.acquisition().fraisNotaireEnCentimes(),
                        r.acquisition().fraisAgenceEnCentimes(),
                        r.acquisition().travauxAlAcquisitionEnCentimes(),
                        r.acquisition().fraisDossierBancaireEnCentimes()
                ),
                new ParametresFinancement(
                        r.financement().montantEmprunteEnCentimes(),
                        r.financement().tauxAnnuelPourcent(),
                        r.financement().dureeAnnees(),
                        r.financement().tauxAssuranceEmprunteurPourcent()
                ),
                new ParametresAmortissement(
                        r.amortissement().quotePartTerrainPourcent(),
                        r.amortissement().quotePartMobilierPourcent(),
                        r.amortissement().dureeAmortissementBatiAnnees(),
                        r.amortissement().dureeAmortissementMobilierAnnees()
                ),
                revenus,
                new ParametresChargesRecurrentes(
                        r.chargesRecurrentes().taxeFonciereEnCentimes(),
                        r.chargesRecurrentes().assurancePnoEnCentimes(),
                        r.chargesRecurrentes().assuranceLoyersImpayesEnCentimes(),
                        r.chargesRecurrentes().fraisGestionLocativePourcentLoyer(),
                        r.chargesRecurrentes().provisionTravauxAnnuelleEnCentimes(),
                        r.chargesRecurrentes().fraisComptabiliteAnnuelEnCentimes(),
                        r.chargesRecurrentes().chargesCoproprieteNonRecuperablesEnCentimes()
                ),
                new HypothesesEvolution(
                        r.hypothesesEvolution().tauxVacanceLocativePourcent(),
                        r.hypothesesEvolution().tauxIndexationLoyerPourcent(),
                        r.hypothesesEvolution().tauxIndexationChargesPourcent()
                )
        );
    }
}

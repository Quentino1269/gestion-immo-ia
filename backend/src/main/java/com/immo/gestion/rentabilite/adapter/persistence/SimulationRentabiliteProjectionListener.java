package com.immo.gestion.rentabilite.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneProjection;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteModifiee;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Maintient la projection {@code simulations_rentabilite} à jour à partir des événements de
 * l'event store. {@code @EventListener} volontairement synchrone (pas
 * {@code @TransactionalEventListener}) pour s'exécuter dans la même transaction que l'append.
 * Cf. MISSION.md §5. Toujours l'état COURANT (dernière version) ; l'historique complet reste dans
 * l'event store, rejoué à la demande par {@link SimulationRentabiliteRepositoryAdapter}.
 */
@Component
public class SimulationRentabiliteProjectionListener {

    private final SimulationRentabiliteJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public SimulationRentabiliteProjectionListener(SimulationRentabiliteJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void surRentabiliteSimulee(RentabiliteSimulee evenement) {
        jpa.save(construire(
                evenement.simulationId().valeur(), evenement.bienId().valeur(), evenement.utilisateurId().valeur(),
                evenement.nomScenario(), evenement.regimeFiscal(), evenement.tmiFoyerPourcent(), evenement.horizonAnnees(),
                evenement.acquisition(), evenement.financement(), evenement.amortissement(),
                evenement.revenusLocatifsSimules(), evenement.chargesRecurrentes(), evenement.hypothesesEvolution(),
                evenement.coutTotalAcquisitionEnCentimes(), evenement.apportPersonnelEnCentimes(),
                evenement.projectionAnnuelle(), evenement.survenuLe()
        ));
    }

    @EventListener
    public void surSimulationRentabiliteModifiee(SimulationRentabiliteModifiee evenement) {
        SimulationRentabiliteEntity existante = jpa.findById(evenement.simulationId().valeur())
                .orElseThrow(() -> new IllegalStateException(
                        "Projection simulations_rentabilite absente pour " + evenement.simulationId()
                                + " — SimulationRentabiliteModifiee ne peut pas être le premier événement du flux"));
        jpa.save(construire(
                existante.getId(), existante.getBienId(), existante.getUtilisateurId(),
                evenement.nomScenario(), evenement.regimeFiscal(), evenement.tmiFoyerPourcent(), evenement.horizonAnnees(),
                evenement.acquisition(), evenement.financement(), evenement.amortissement(),
                evenement.revenusLocatifsSimules(), evenement.chargesRecurrentes(), evenement.hypothesesEvolution(),
                evenement.coutTotalAcquisitionEnCentimes(), evenement.apportPersonnelEnCentimes(),
                evenement.projectionAnnuelle(), evenement.survenuLe()
        ));
    }

    private SimulationRentabiliteEntity construire(
            UUID id, UUID bienId, UUID utilisateurId, String nomScenario, RegimeFiscal regimeFiscal,
            int tmiFoyerPourcent, int horizonAnnees,
            ParametresAcquisition acquisition, ParametresFinancement financement, ParametresAmortissement amortissement,
            List<LigneRevenuSimule> revenusLocatifsSimules, ParametresChargesRecurrentes chargesRecurrentes,
            HypothesesEvolution hypothesesEvolution,
            long coutTotalAcquisitionEnCentimes, long apportPersonnelEnCentimes,
            List<LigneProjection> projectionAnnuelle, Instant simuleLe
    ) {
        return new SimulationRentabiliteEntity(
                id, bienId, utilisateurId, nomScenario, regimeFiscal, tmiFoyerPourcent, horizonAnnees,
                acquisition.prixAchatEnCentimes(), acquisition.fraisNotaireEnCentimes(), acquisition.fraisAgenceEnCentimes(),
                acquisition.travauxAlAcquisitionEnCentimes(), acquisition.fraisDossierBancaireEnCentimes(),
                financement.montantEmprunteEnCentimes(), financement.tauxAnnuelPourcent(), financement.dureeAnnees(),
                financement.tauxAssuranceEmprunteurPourcent(),
                amortissement.quotePartTerrainPourcent(), amortissement.quotePartMobilierPourcent(),
                amortissement.dureeAmortissementBatiAnnees(), amortissement.dureeAmortissementMobilierAnnees(),
                chargesRecurrentes.taxeFonciereEnCentimes(), chargesRecurrentes.assurancePnoEnCentimes(),
                chargesRecurrentes.assuranceLoyersImpayesEnCentimes(), chargesRecurrentes.fraisGestionLocativePourcentLoyer(),
                chargesRecurrentes.provisionTravauxAnnuelleEnCentimes(), chargesRecurrentes.fraisComptabiliteAnnuelEnCentimes(),
                chargesRecurrentes.chargesCoproprieteNonRecuperablesEnCentimes(),
                hypothesesEvolution.tauxVacanceLocativePourcent(), hypothesesEvolution.tauxIndexationLoyerPourcent(),
                hypothesesEvolution.tauxIndexationChargesPourcent(),
                coutTotalAcquisitionEnCentimes, apportPersonnelEnCentimes,
                ecrire(revenusLocatifsSimules), ecrire(projectionAnnuelle),
                simuleLe
        );
    }

    private String ecrire(Object valeur) {
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation impossible pour la projection simulations_rentabilite", e);
        }
    }
}

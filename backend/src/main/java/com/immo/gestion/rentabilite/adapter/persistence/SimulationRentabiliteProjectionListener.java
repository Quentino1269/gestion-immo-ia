package com.immo.gestion.rentabilite.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.rentabilite.domain.RentabiliteSimulee;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Maintient la projection {@code simulations_rentabilite} à jour à partir des événements de
 * l'event store. {@code @EventListener} volontairement synchrone (pas
 * {@code @TransactionalEventListener}) pour s'exécuter dans la même transaction que l'append.
 * Cf. MISSION.md §5.
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
        jpa.save(new SimulationRentabiliteEntity(
                evenement.simulationId().valeur(),
                evenement.bienId().valeur(),
                evenement.utilisateurId().valeur(),
                evenement.nomScenario(),
                evenement.regimeFiscal(),
                evenement.tmiFoyerPourcent(),
                evenement.horizonAnnees(),
                evenement.acquisition().prixAchatEnCentimes(),
                evenement.acquisition().fraisNotaireEnCentimes(),
                evenement.acquisition().fraisAgenceEnCentimes(),
                evenement.acquisition().travauxAlAcquisitionEnCentimes(),
                evenement.acquisition().fraisDossierBancaireEnCentimes(),
                evenement.financement().montantEmprunteEnCentimes(),
                evenement.financement().tauxAnnuelPourcent(),
                evenement.financement().dureeAnnees(),
                evenement.financement().tauxAssuranceEmprunteurPourcent(),
                evenement.amortissement().quotePartTerrainPourcent(),
                evenement.amortissement().quotePartMobilierPourcent(),
                evenement.amortissement().dureeAmortissementBatiAnnees(),
                evenement.amortissement().dureeAmortissementMobilierAnnees(),
                evenement.chargesRecurrentes().taxeFonciereEnCentimes(),
                evenement.chargesRecurrentes().assurancePnoEnCentimes(),
                evenement.chargesRecurrentes().assuranceLoyersImpayesEnCentimes(),
                evenement.chargesRecurrentes().fraisGestionLocativePourcentLoyer(),
                evenement.chargesRecurrentes().provisionTravauxAnnuelleEnCentimes(),
                evenement.chargesRecurrentes().fraisComptabiliteAnnuelEnCentimes(),
                evenement.chargesRecurrentes().chargesCoproprieteNonRecuperablesEnCentimes(),
                evenement.hypothesesEvolution().tauxVacanceLocativePourcent(),
                evenement.hypothesesEvolution().tauxIndexationLoyerPourcent(),
                evenement.hypothesesEvolution().tauxIndexationChargesPourcent(),
                evenement.coutTotalAcquisitionEnCentimes(),
                evenement.apportPersonnelEnCentimes(),
                ecrire(evenement.revenusLocatifsSimules()),
                ecrire(evenement.projectionAnnuelle()),
                evenement.survenuLe()
        ));
    }

    private String ecrire(Object valeur) {
        try {
            return objectMapper.writeValueAsString(valeur);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation impossible pour la projection simulations_rentabilite", e);
        }
    }
}

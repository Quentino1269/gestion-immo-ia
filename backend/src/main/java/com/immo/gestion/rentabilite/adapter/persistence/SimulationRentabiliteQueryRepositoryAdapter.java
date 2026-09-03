package com.immo.gestion.rentabilite.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneProjection;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.SimulationRentabilite;
import com.immo.gestion.rentabilite.domain.SimulationRentabiliteId;
import com.immo.gestion.rentabilite.domain.port.out.SimulationRentabiliteQueryRepository;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Lecture adossée à la projection {@code simulations_rentabilite}, maintenue à jour par
 * {@link SimulationRentabiliteProjectionListener}.
 */
@Repository
public class SimulationRentabiliteQueryRepositoryAdapter implements SimulationRentabiliteQueryRepository {

    private final SimulationRentabiliteJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public SimulationRentabiliteQueryRepositoryAdapter(SimulationRentabiliteJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SimulationRentabilite> chargerParId(SimulationRentabiliteId id) {
        return jpa.findById(id.valeur()).map(this::versDomaine);
    }

    @Override
    public List<SimulationRentabilite> chargerParBien(BienId bienId) {
        return jpa.findByBienId(bienId.valeur()).stream()
                .map(this::versDomaine)
                .toList();
    }

    private SimulationRentabilite versDomaine(SimulationRentabiliteEntity e) {
        return new SimulationRentabilite(
                new SimulationRentabiliteId(e.getId()),
                new BienId(e.getBienId()),
                new UtilisateurId(e.getUtilisateurId()),
                e.getNomScenario(),
                e.getRegimeFiscal(),
                e.getTmiFoyerPourcent(),
                e.getHorizonAnnees(),
                new ParametresAcquisition(
                        e.getPrixAchatEnCentimes(),
                        e.getFraisNotaireEnCentimes(),
                        e.getFraisAgenceEnCentimes(),
                        e.getTravauxAlAcquisitionEnCentimes(),
                        e.getFraisDossierBancaireEnCentimes()
                ),
                new ParametresFinancement(
                        e.getMontantEmprunteEnCentimes(),
                        e.getTauxAnnuelPourcent(),
                        e.getDureeAnnees(),
                        e.getTauxAssuranceEmprunteurPourcent()
                ),
                new ParametresAmortissement(
                        e.getQuotePartTerrainPourcent(),
                        e.getQuotePartMobilierPourcent(),
                        e.getDureeAmortissementBatiAnnees(),
                        e.getDureeAmortissementMobilierAnnees()
                ),
                lire(e.getRevenusLocatifsSimulesJson(), new TypeReference<List<LigneRevenuSimule>>() {}),
                new ParametresChargesRecurrentes(
                        e.getTaxeFonciereEnCentimes(),
                        e.getAssurancePnoEnCentimes(),
                        e.getAssuranceLoyersImpayesEnCentimes(),
                        e.getFraisGestionLocativePourcentLoyer(),
                        e.getProvisionTravauxAnnuelleEnCentimes(),
                        e.getFraisComptabiliteAnnuelEnCentimes(),
                        e.getChargesCoproprieteNonRecuperablesEnCentimes()
                ),
                new HypothesesEvolution(
                        e.getTauxVacanceLocativePourcent(),
                        e.getTauxIndexationLoyerPourcent(),
                        e.getTauxIndexationChargesPourcent()
                ),
                e.getCoutTotalAcquisitionEnCentimes(),
                e.getApportPersonnelEnCentimes(),
                lire(e.getProjectionAnnuelleJson(), new TypeReference<List<LigneProjection>>() {}),
                e.getSimuleLe(),
                false
        );
    }

    private <T> T lire(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Désérialisation impossible depuis simulations_rentabilite", e);
        }
    }
}

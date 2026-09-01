package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate SimulationRentabilite — fait immuable, un scénario = un événement (D2).
 * Invariants I-SIM-4, I-SIM-5, I-SIM-7, I-SIM-17 (les autres, cross-aggregate avec Bien,
 * sont vérifiés par SimulationRentabiliteService avant construction).
 * Cf. docs/slices/projection-rentabilite.md §9.
 */
public record SimulationRentabilite(
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
        HypothesesEvolution hypothesesEvolution,
        long coutTotalAcquisitionEnCentimes,
        long apportPersonnelEnCentimes,
        List<LigneProjection> projectionAnnuelle,
        Instant simuleLe
) {

    private static final int NOM_SCENARIO_MAX = 100;
    private static final List<Integer> TMI_VALIDES = List.of(0, 11, 30, 41, 45);

    public SimulationRentabilite {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(bienId, "bienId requis");
        Objects.requireNonNull(utilisateurId, "utilisateurId requis");
        // I-SIM-17
        if (nomScenario == null || nomScenario.isBlank()) {
            throw new IllegalArgumentException("nomScenario requis");
        }
        nomScenario = nomScenario.strip();
        if (nomScenario.length() > NOM_SCENARIO_MAX) {
            throw new IllegalArgumentException("nomScenario trop long (max " + NOM_SCENARIO_MAX + " car.)");
        }
        Objects.requireNonNull(regimeFiscal, "regimeFiscal requis");
        // I-SIM-4
        if (!TMI_VALIDES.contains(tmiFoyerPourcent)) {
            throw new IllegalArgumentException("tmiFoyerPourcent doit être 0, 11, 30, 41 ou 45");
        }
        // I-SIM-5
        if (horizonAnnees < 1 || horizonAnnees > 40) {
            throw new IllegalArgumentException("horizonAnnees doit être dans [1,40]");
        }
        Objects.requireNonNull(acquisition, "acquisition requise");
        Objects.requireNonNull(financement, "financement requis");
        Objects.requireNonNull(amortissement, "amortissement requis");
        Objects.requireNonNull(chargesRecurrentes, "chargesRecurrentes requises");
        Objects.requireNonNull(hypothesesEvolution, "hypothesesEvolution requises");
        // I-SIM-11 (forme minimale ; l'exhaustivité vs. les chambres du bien est vérifiée par le service)
        if (revenusLocatifsSimules == null || revenusLocatifsSimules.isEmpty()) {
            throw new IllegalArgumentException("revenusLocatifsSimules ne peut pas être vide");
        }
        revenusLocatifsSimules = List.copyOf(revenusLocatifsSimules);
        if (coutTotalAcquisitionEnCentimes != acquisition.coutTotalEnCentimes()) {
            throw new IllegalArgumentException("coutTotalAcquisitionEnCentimes incohérent avec acquisition");
        }
        // I-SIM-7
        if (financement.montantEmprunteEnCentimes() > coutTotalAcquisitionEnCentimes) {
            throw new IllegalArgumentException("montantEmprunteEnCentimes doit être ≤ coutTotalAcquisitionEnCentimes");
        }
        if (apportPersonnelEnCentimes != coutTotalAcquisitionEnCentimes - financement.montantEmprunteEnCentimes()) {
            throw new IllegalArgumentException("apportPersonnelEnCentimes incohérent");
        }
        Objects.requireNonNull(projectionAnnuelle, "projectionAnnuelle requise");
        if (projectionAnnuelle.size() != horizonAnnees) {
            throw new IllegalArgumentException("projectionAnnuelle doit compter horizonAnnees lignes");
        }
        projectionAnnuelle = List.copyOf(projectionAnnuelle);
        Objects.requireNonNull(simuleLe, "simuleLe requis");
    }
}

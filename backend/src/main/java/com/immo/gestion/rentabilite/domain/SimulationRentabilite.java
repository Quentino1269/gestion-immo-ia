package com.immo.gestion.rentabilite.domain;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Instant;
import java.util.ArrayList;
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
        Instant simuleLe,
        boolean supprimee
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

    /**
     * Reconstruit l'état courant par rejeu du flux (Event Sourcing, MISSION §5) : un
     * {@link RentabiliteSimulee} initial, puis zéro ou plusieurs {@link SimulationRentabiliteModifiee}
     * qui écrasent les champs modifiables (jamais {@code id}/{@code bienId}/{@code utilisateurId}).
     */
    public static SimulationRentabilite reconstruire(List<DomainEvent> evenements) {
        List<SimulationRentabilite> historique = reconstruireHistorique(evenements);
        return historique.get(historique.size() - 1);
    }

    /**
     * Comme {@link #reconstruire}, mais retourne l'état après chaque événement plutôt que le seul
     * état final — sert à afficher l'historique des versions d'une simulation.
     */
    public static List<SimulationRentabilite> reconstruireHistorique(List<DomainEvent> evenements) {
        if (evenements.isEmpty()) {
            throw new IllegalStateException("Flux SimulationRentabilite vide : impossible de reconstruire l'historique");
        }
        List<SimulationRentabilite> historique = new ArrayList<>(evenements.size());
        SimulationRentabilite etat = null;
        for (DomainEvent evenement : evenements) {
            etat = appliquer(etat, evenement);
            historique.add(etat);
        }
        return historique;
    }

    private static SimulationRentabilite appliquer(SimulationRentabilite etat, DomainEvent evenement) {
        if (etat == null && !(evenement instanceof RentabiliteSimulee)) {
            throw new IllegalStateException(
                    "Flux SimulationRentabilite corrompu : premier événement attendu RentabiliteSimulee, reçu "
                            + evenement.getClass());
        }
        return switch (evenement) {
            case RentabiliteSimulee e -> new SimulationRentabilite(
                    e.simulationId(), e.bienId(), e.utilisateurId(), e.nomScenario(), e.regimeFiscal(),
                    e.tmiFoyerPourcent(), e.horizonAnnees(), e.acquisition(), e.financement(), e.amortissement(),
                    e.revenusLocatifsSimules(), e.chargesRecurrentes(), e.hypothesesEvolution(),
                    e.coutTotalAcquisitionEnCentimes(), e.apportPersonnelEnCentimes(), e.projectionAnnuelle(),
                    e.survenuLe(), false
            );
            case SimulationRentabiliteModifiee e -> new SimulationRentabilite(
                    etat.id(), etat.bienId(), etat.utilisateurId(), e.nomScenario(), e.regimeFiscal(),
                    e.tmiFoyerPourcent(), e.horizonAnnees(), e.acquisition(), e.financement(), e.amortissement(),
                    e.revenusLocatifsSimules(), e.chargesRecurrentes(), e.hypothesesEvolution(),
                    e.coutTotalAcquisitionEnCentimes(), e.apportPersonnelEnCentimes(), e.projectionAnnuelle(),
                    e.survenuLe(), etat.supprimee()
            );
            case SimulationRentabiliteSupprimee e -> new SimulationRentabilite(
                    etat.id(), etat.bienId(), etat.utilisateurId(), etat.nomScenario(), etat.regimeFiscal(),
                    etat.tmiFoyerPourcent(), etat.horizonAnnees(), etat.acquisition(), etat.financement(),
                    etat.amortissement(), etat.revenusLocatifsSimules(), etat.chargesRecurrentes(),
                    etat.hypothesesEvolution(), etat.coutTotalAcquisitionEnCentimes(), etat.apportPersonnelEnCentimes(),
                    etat.projectionAnnuelle(), e.survenuLe(), true
            );
            default -> throw new IllegalStateException(
                    "Événement inattendu dans le flux SimulationRentabilite : " + evenement.getClass());
        };
    }
}

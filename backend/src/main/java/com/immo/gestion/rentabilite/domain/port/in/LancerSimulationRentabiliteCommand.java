package com.immo.gestion.rentabilite.domain.port.in;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.rentabilite.domain.HypothesesEvolution;
import com.immo.gestion.rentabilite.domain.LigneRevenuSimule;
import com.immo.gestion.rentabilite.domain.ParametresAcquisition;
import com.immo.gestion.rentabilite.domain.ParametresAmortissement;
import com.immo.gestion.rentabilite.domain.ParametresChargesRecurrentes;
import com.immo.gestion.rentabilite.domain.ParametresFinancement;
import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.util.List;

public record LancerSimulationRentabiliteCommand(
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
}

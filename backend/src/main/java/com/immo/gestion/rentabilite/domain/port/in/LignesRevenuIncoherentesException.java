package com.immo.gestion.rentabilite.domain.port.in;

/**
 * I-SIM-11 : les lignes de revenu simulé doivent correspondre exactement au bien racine
 * lui-même (s'il n'a pas de chambre active) ou à l'ensemble exhaustif de ses chambres actives.
 */
public class LignesRevenuIncoherentesException extends RuntimeException {

    public LignesRevenuIncoherentesException(String detail) {
        super("Lignes de revenu locatif incohérentes avec le bien : " + detail);
    }
}

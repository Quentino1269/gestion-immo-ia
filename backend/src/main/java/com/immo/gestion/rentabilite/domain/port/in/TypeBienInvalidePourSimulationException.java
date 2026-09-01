package com.immo.gestion.rentabilite.domain.port.in;

/** I-SIM-1 : le bien simulé doit être un bien racine (MAISON ou APPARTEMENT). */
public class TypeBienInvalidePourSimulationException extends RuntimeException {

    public TypeBienInvalidePourSimulationException() {
        super("Une simulation de rentabilité ne peut porter que sur un bien racine (MAISON ou APPARTEMENT)");
    }
}

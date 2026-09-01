package com.immo.gestion.rentabilite.domain.port.in;

/** I-SIM-3 : le régime fiscal doit être cohérent avec le caractère meublé du bien. */
public class RegimeFiscalIncoherentException extends RuntimeException {

    public RegimeFiscalIncoherentException() {
        super("Le régime fiscal choisi est incohérent avec le caractère meublé du bien");
    }
}

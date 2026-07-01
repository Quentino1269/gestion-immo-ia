package com.immo.gestion.bien.domain.port.in;

public class DroitInsuffisantSurParentException extends RuntimeException {

    public DroitInsuffisantSurParentException() {
        super("Le propriétaire n'est pas le propriétaire du bien parent");
    }
}

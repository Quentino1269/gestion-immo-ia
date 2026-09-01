package com.immo.gestion.bien.domain.port.in;

public class DroitInsuffisantSurBienException extends RuntimeException {

    public DroitInsuffisantSurBienException() {
        super("Vous n'êtes pas ayant droit de ce bien");
    }
}

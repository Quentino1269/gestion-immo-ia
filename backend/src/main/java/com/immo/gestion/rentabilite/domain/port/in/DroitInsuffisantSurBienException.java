package com.immo.gestion.rentabilite.domain.port.in;

/** I-SIM-2 : seul un ayant droit du bien peut lancer/consulter une simulation le concernant. */
public class DroitInsuffisantSurBienException extends RuntimeException {

    public DroitInsuffisantSurBienException() {
        super("Vous n'êtes pas ayant droit de ce bien");
    }
}

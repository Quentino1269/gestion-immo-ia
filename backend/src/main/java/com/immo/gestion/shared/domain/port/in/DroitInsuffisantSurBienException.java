package com.immo.gestion.shared.domain.port.in;

/** Seul l'ayant droit d'un bien peut agir sur ce bien ou sur ce qui en dépend (ex. simulations). */
public class DroitInsuffisantSurBienException extends RuntimeException {

    public DroitInsuffisantSurBienException() {
        super("Vous n'êtes pas ayant droit de ce bien");
    }
}

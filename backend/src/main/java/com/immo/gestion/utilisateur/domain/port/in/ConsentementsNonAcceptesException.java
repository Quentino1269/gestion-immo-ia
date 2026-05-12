package com.immo.gestion.utilisateur.domain.port.in;

/**
 * Levée si CGU ou politique de confidentialité ne sont pas acceptées (I-5).
 */
public class ConsentementsNonAcceptesException extends RuntimeException {

    public ConsentementsNonAcceptesException() {
        super("vous devez accepter les CGU et la politique de confidentialité");
    }
}

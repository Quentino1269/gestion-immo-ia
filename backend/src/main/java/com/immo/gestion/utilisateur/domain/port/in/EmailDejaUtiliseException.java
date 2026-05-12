package com.immo.gestion.utilisateur.domain.port.in;

/**
 * Levée lorsqu'un email est déjà utilisé. Le message API reste générique
 * (anti-énumération, cf. docs/slices/creation-utilisateur.md §11).
 */
public class EmailDejaUtiliseException extends RuntimeException {

    public EmailDejaUtiliseException() {
        super("cette adresse ne peut pas être utilisée");
    }
}

package com.immo.gestion.session.domain.port.in;

public class SessionInvalideException extends RuntimeException {

    public SessionInvalideException() {
        super("session invalide");
    }
}

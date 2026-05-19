package com.immo.gestion.session.adapter.web;

import com.immo.gestion.session.domain.port.in.IdentifiantsInvalidesException;
import com.immo.gestion.session.domain.port.in.SessionInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Réponses HTTP du bounded context session. Toutes les erreurs de login
 * renvoient un message <b>strictement identique</b> (D7, anti-énumération) :
 * peu importe que l'email soit inconnu, le mot de passe faux, le compte
 * inactif ou la requête mal formée.
 */
@RestControllerAdvice(assignableTypes = SessionController.class)
public class SessionExceptionHandler {

    static final String MESSAGE_GENERIQUE = "Identifiants invalides.";

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<SessionApiError> identifiantsInvalides(IdentifiantsInvalidesException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SessionApiError(MESSAGE_GENERIQUE));
    }

    @ExceptionHandler(SessionInvalideException.class)
    public ResponseEntity<SessionApiError> sessionInvalide(SessionInvalideException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SessionApiError(MESSAGE_GENERIQUE));
    }

    /**
     * Email ou mot de passe absent du body, etc. — ne pas révéler de détail.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SessionApiError> beanValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SessionApiError(MESSAGE_GENERIQUE));
    }

    /**
     * Émis par les constructeurs des VO ({@link com.immo.gestion.shared.MotDePasseSoumis},
     * etc.) si une valeur ne respecte pas l'invariant. Même message générique
     * pour ne pas trahir le motif.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SessionApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SessionApiError(MESSAGE_GENERIQUE));
    }
}

package com.immo.gestion.utilisateur.adapter.web;

import com.immo.gestion.utilisateur.domain.port.in.ConsentementsNonAcceptesException;
import com.immo.gestion.utilisateur.domain.port.in.EmailDejaUtiliseException;
import com.immo.gestion.utilisateur.domain.port.in.ModificationProfilRefuseeException;
import com.immo.gestion.utilisateur.domain.port.in.UtilisateurNonTrouveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(assignableTypes = UtilisateurController.class)
public class UtilisateurExceptionHandler {

    private static final String MESSAGE_GENERIQUE_DEDOUBLON =
            "Cette adresse ne peut pas être utilisée.";

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ApiError> emailDejaUtilise(EmailDejaUtiliseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.simple(MESSAGE_GENERIQUE_DEDOUBLON));
    }

    @ExceptionHandler(ConsentementsNonAcceptesException.class)
    public ResponseEntity<ApiError> consentementsNonAcceptes(ConsentementsNonAcceptesException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(ModificationProfilRefuseeException.class)
    public ResponseEntity<ApiError> modificationRefusee(ModificationProfilRefuseeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(UtilisateurNonTrouveException.class)
    public ResponseEntity<ApiError> utilisateurNonTrouve(UtilisateurNonTrouveException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> beanValidation(MethodArgumentNotValidException ex) {
        List<ApiError.ChampEnErreur> erreurs = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.ChampEnErreur(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Requête invalide.", erreurs));
    }
}

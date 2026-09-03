package com.immo.gestion.bien.adapter.web;

import com.immo.gestion.bien.domain.port.in.BienNonTrouveException;
import com.immo.gestion.bien.domain.port.in.BienParentIntrouvableException;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurParentException;
import com.immo.gestion.bien.domain.port.in.LibelleChambreNonUniqueException;
import com.immo.gestion.bien.domain.port.in.SurfaceChambresDepasseeException;
import com.immo.gestion.shared.domain.port.out.ConflitDeVersionException;
import com.immo.gestion.utilisateur.adapter.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BienController.class)
public class BienExceptionHandler {

    @ExceptionHandler(BienNonTrouveException.class)
    public ResponseEntity<ApiError> bienNonTrouve(BienNonTrouveException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(BienParentIntrouvableException.class)
    public ResponseEntity<ApiError> parentIntrouvable(BienParentIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(DroitInsuffisantSurParentException.class)
    public ResponseEntity<ApiError> droitInsuffisant(DroitInsuffisantSurParentException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(DroitInsuffisantSurBienException.class)
    public ResponseEntity<ApiError> droitInsuffisantSurBien(DroitInsuffisantSurBienException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(LibelleChambreNonUniqueException.class)
    public ResponseEntity<ApiError> libelleNonUnique(LibelleChambreNonUniqueException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(SurfaceChambresDepasseeException.class)
    public ResponseEntity<ApiError> surfaceDepassee(SurfaceChambresDepasseeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.simple(ex.getMessage()));
    }

    /**
     * Concurrence optimiste (Event Sourcing, MISSION §5) : atteignable en pratique depuis l'ajout
     * de la modification de bien (ModifierBienUseCase) — deux PUT concurrents sur le même bien
     * peuvent se disputer la même version chargée.
     */
    @ExceptionHandler(ConflitDeVersionException.class)
    public ResponseEntity<ApiError> conflitDeVersion(ConflitDeVersionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.simple("Ce bien a été modifié entre-temps, veuillez réessayer."));
    }
}

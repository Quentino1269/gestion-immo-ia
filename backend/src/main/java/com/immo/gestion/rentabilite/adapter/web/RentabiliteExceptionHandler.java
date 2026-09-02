package com.immo.gestion.rentabilite.adapter.web;

import com.immo.gestion.rentabilite.domain.port.in.BienNonTrouveException;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.rentabilite.domain.port.in.LignesRevenuIncoherentesException;
import com.immo.gestion.rentabilite.domain.port.in.RegimeFiscalIncoherentException;
import com.immo.gestion.rentabilite.domain.port.in.SimulationNonTrouveeException;
import com.immo.gestion.rentabilite.domain.port.in.TypeBienInvalidePourSimulationException;
import com.immo.gestion.shared.domain.port.out.ConflitDeVersionException;
import com.immo.gestion.utilisateur.adapter.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RentabiliteController.class)
public class RentabiliteExceptionHandler {

    @ExceptionHandler(BienNonTrouveException.class)
    public ResponseEntity<ApiError> bienNonTrouve(BienNonTrouveException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(SimulationNonTrouveeException.class)
    public ResponseEntity<ApiError> simulationNonTrouvee(SimulationNonTrouveeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(DroitInsuffisantSurBienException.class)
    public ResponseEntity<ApiError> droitInsuffisant(DroitInsuffisantSurBienException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(TypeBienInvalidePourSimulationException.class)
    public ResponseEntity<ApiError> typeBienInvalide(TypeBienInvalidePourSimulationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(RegimeFiscalIncoherentException.class)
    public ResponseEntity<ApiError> regimeIncoherent(RegimeFiscalIncoherentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(LignesRevenuIncoherentesException.class)
    public ResponseEntity<ApiError> lignesRevenuIncoherentes(LignesRevenuIncoherentesException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.simple(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.simple(ex.getMessage()));
    }

    /**
     * Concurrence optimiste (Event Sourcing, MISSION §5). Pas encore atteignable en pratique
     * (SimulationRentabilite est create-only en V1, D2), mappé par cohérence.
     */
    @ExceptionHandler(ConflitDeVersionException.class)
    public ResponseEntity<ApiError> conflitDeVersion(ConflitDeVersionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.simple("Cette simulation a été modifiée entre-temps, veuillez réessayer."));
    }
}

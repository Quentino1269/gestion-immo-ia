package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.BienId;

public class BienNonTrouveException extends RuntimeException {

    public BienNonTrouveException(BienId id) {
        super("Bien introuvable : " + id.valeur());
    }
}

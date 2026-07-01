package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.BienId;

public class BienParentIntrouvableException extends RuntimeException {

    public BienParentIntrouvableException(BienId parentId) {
        super("Bien parent introuvable : " + parentId.valeur());
    }
}

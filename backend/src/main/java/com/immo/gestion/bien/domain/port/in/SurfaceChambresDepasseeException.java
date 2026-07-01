package com.immo.gestion.bien.domain.port.in;

public class SurfaceChambresDepasseeException extends RuntimeException {

    public SurfaceChambresDepasseeException() {
        super("La somme des surfaces des chambres dépasse la surface du bien parent");
    }
}

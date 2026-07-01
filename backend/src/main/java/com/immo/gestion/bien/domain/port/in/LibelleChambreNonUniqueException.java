package com.immo.gestion.bien.domain.port.in;

public class LibelleChambreNonUniqueException extends RuntimeException {

    public LibelleChambreNonUniqueException(String libelle) {
        super("Une chambre avec le libellé « " + libelle + " » existe déjà dans ce bien parent");
    }
}

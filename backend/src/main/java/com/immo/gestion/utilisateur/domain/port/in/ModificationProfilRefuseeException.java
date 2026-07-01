package com.immo.gestion.utilisateur.domain.port.in;

/**
 * Levée quand un champ déjà renseigné est soumis avec une valeur différente (I-10).
 * La modification appartient au slice « Modification du profil » (D6).
 */
public class ModificationProfilRefuseeException extends RuntimeException {

    private final String champ;

    public ModificationProfilRefuseeException(String champ) {
        super("Le champ « " + champ + " » est déjà renseigné et ne peut pas être modifié ici.");
        this.champ = champ;
    }

    public String champ() {
        return champ;
    }
}

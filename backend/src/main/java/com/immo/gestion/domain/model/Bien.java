package com.immo.gestion.domain.model;

import java.util.UUID;

public record Bien(
        UUID id,
        String reference,
        String adresse,
        TypeBien type,
        double surface,
        long prixEnCentimes
) {
    public Bien {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("La référence du bien est obligatoire");
        }
        if (adresse == null || adresse.isBlank()) {
            throw new IllegalArgumentException("L'adresse du bien est obligatoire");
        }
        if (surface <= 0) {
            throw new IllegalArgumentException("La surface doit être strictement positive");
        }
        if (prixEnCentimes < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas être négatif");
        }
    }
}

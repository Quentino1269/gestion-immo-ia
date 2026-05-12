package com.immo.gestion.domain.port.out;

import com.immo.gestion.domain.model.Bien;

import java.util.Optional;
import java.util.UUID;

public interface BienRepository {

    Bien sauvegarder(Bien bien);

    Optional<Bien> rechercherParId(UUID id);

    Optional<Bien> rechercherParReference(String reference);
}

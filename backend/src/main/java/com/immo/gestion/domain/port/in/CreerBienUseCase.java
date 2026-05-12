package com.immo.gestion.domain.port.in;

import com.immo.gestion.domain.model.Bien;
import com.immo.gestion.domain.model.TypeBien;

public interface CreerBienUseCase {

    Bien creer(CreerBienCommand command);

    record CreerBienCommand(
            String reference,
            String adresse,
            TypeBien type,
            double surface,
            long prixEnCentimes
    ) {}
}

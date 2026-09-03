package com.immo.gestion.bien.domain.port.in;

import com.immo.gestion.bien.domain.FicheBien;

public interface ModifierBienUseCase {

    FicheBien modifier(ModifierBienCommand commande);
}

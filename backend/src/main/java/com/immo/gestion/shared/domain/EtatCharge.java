package com.immo.gestion.shared.domain;

/**
 * État d'un aggregate reconstruit par rejeu, accompagné de la version du flux
 * au moment du chargement (contrôle de concurrence optimiste à l'écriture).
 */
public record EtatCharge<T>(T aggregat, long version) {
}

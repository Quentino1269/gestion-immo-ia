package com.immo.gestion.session.domain.port.in;

public interface SeConnecterUseCase {

    /**
     * @throws IdentifiantsInvalidesException en cas d'échec, quel que soit le motif réel
     *         (D7, anti-énumération : le détail reste interne, conservé dans l'event d'audit).
     */
    SeConnecterResultat seConnecter(SeConnecterCommand commande);
}

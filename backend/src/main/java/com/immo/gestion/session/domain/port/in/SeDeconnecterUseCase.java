package com.immo.gestion.session.domain.port.in;

public interface SeDeconnecterUseCase {

    /**
     * @throws SessionInvalideException si la session n'existe pas, est déjà fermée,
     *         ou n'appartient pas à l'utilisateur appelant (I-6, I-7).
     */
    void seDeconnecter(SeDeconnecterCommand commande);
}

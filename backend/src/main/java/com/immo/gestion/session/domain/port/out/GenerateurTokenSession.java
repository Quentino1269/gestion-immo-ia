package com.immo.gestion.session.domain.port.out;

import com.immo.gestion.session.domain.TokenSession;
import com.immo.gestion.session.domain.TokenSessionGenere;
import com.immo.gestion.session.domain.TokenSessionHash;

/**
 * Générateur de tokens opaques (D2, I-4) : aléatoire fort ≥ 128 bits,
 * hash sha-256 stocké côté serveur.
 */
public interface GenerateurTokenSession {

    TokenSessionGenere genererNouveau();

    /**
     * Hash déterministe utilisé par le filter d'authentification pour
     * résoudre une session à partir du token Bearer reçu.
     */
    TokenSessionHash hasher(TokenSession token);
}

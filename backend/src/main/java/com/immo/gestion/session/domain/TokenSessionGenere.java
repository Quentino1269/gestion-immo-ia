package com.immo.gestion.session.domain;

import java.util.Objects;

/**
 * Couple (token clair, hash) produit par {@link com.immo.gestion.session.domain.port.out.GenerateurTokenSession}.
 * Le token clair n'est transmis qu'une fois (au client) ; le hash est seul persisté.
 */
public record TokenSessionGenere(TokenSession clair, TokenSessionHash hash) {

    public TokenSessionGenere {
        Objects.requireNonNull(clair, "token clair requis");
        Objects.requireNonNull(hash, "hash requis");
    }
}

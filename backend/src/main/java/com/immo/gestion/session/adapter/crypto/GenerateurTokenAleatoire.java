package com.immo.gestion.session.adapter.crypto;

import com.immo.gestion.session.domain.TokenSession;
import com.immo.gestion.session.domain.TokenSessionGenere;
import com.immo.gestion.session.domain.TokenSessionHash;
import com.immo.gestion.session.domain.port.out.GenerateurTokenSession;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token opaque base64url issu d'un CSPRNG. 32 octets = 256 bits d'entropie,
 * bien au-delà du minimum de 128 bits exigé par I-4. Hash sha-256 hex
 * minuscule pour le stockage et la résolution côté filter (D2).
 */
@Component
public class GenerateurTokenAleatoire implements GenerateurTokenSession {

    private static final int TAILLE_OCTETS = 32;
    private static final Base64.Encoder ENCODEUR = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public TokenSessionGenere genererNouveau() {
        byte[] aleatoire = new byte[TAILLE_OCTETS];
        secureRandom.nextBytes(aleatoire);
        String clair = ENCODEUR.encodeToString(aleatoire);
        TokenSession token = new TokenSession(clair);
        return new TokenSessionGenere(token, hasher(token));
    }

    @Override
    public TokenSessionHash hasher(TokenSession token) {
        byte[] digest = sha256(token.valeur().getBytes(StandardCharsets.UTF_8));
        return new TokenSessionHash(HexFormat.of().formatHex(digest));
    }

    private static byte[] sha256(byte[] entree) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(entree);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM", e);
        }
    }
}

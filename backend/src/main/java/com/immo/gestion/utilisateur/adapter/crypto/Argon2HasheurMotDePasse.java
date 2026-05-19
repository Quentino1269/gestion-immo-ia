package com.immo.gestion.utilisateur.adapter.crypto;

import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;
import com.immo.gestion.shared.MotDePasseSoumis;
import com.immo.gestion.utilisateur.config.Argon2Config;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import de.mkammerer.argon2.Argon2;
import org.springframework.stereotype.Component;

@Component
public class Argon2HasheurMotDePasse implements HasheurMotDePasse {

    private final Argon2 argon2;

    public Argon2HasheurMotDePasse(Argon2 argon2) {
        this.argon2 = argon2;
    }

    @Override
    public HashMotDePasse hasher(MotDePasseClair motDePasse) {
        char[] caracteres = motDePasse.caracteres();
        try {
            String phc = argon2.hash(
                    Argon2Config.ITERATIONS,
                    Argon2Config.MEMOIRE_KIB,
                    Argon2Config.PARALLELISME,
                    caracteres
            );
            return new HashMotDePasse(phc);
        } finally {
            argon2.wipeArray(caracteres);
        }
    }

    @Override
    public boolean verifier(HashMotDePasse hash, MotDePasseClair candidat) {
        char[] caracteres = candidat.caracteres();
        try {
            return argon2.verify(hash.phcEncoded(), caracteres);
        } finally {
            argon2.wipeArray(caracteres);
        }
    }

    @Override
    public boolean verifierSoumission(HashMotDePasse hash, MotDePasseSoumis soumission) {
        char[] caracteres = soumission.caracteres();
        try {
            return argon2.verify(hash.phcEncoded(), caracteres);
        } finally {
            argon2.wipeArray(caracteres);
        }
    }
}

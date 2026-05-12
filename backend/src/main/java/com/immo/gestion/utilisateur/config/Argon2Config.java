package com.immo.gestion.utilisateur.config;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration argon2id avec paramètres OWASP 2024.
 * Cf. docs/slices/creation-utilisateur.md D8.
 */
@Configuration
public class Argon2Config {

    /** Itérations (t) : ≥ 3. */
    public static final int ITERATIONS = 3;
    /** Mémoire (m) en KiB : ≥ 64 Mo. */
    public static final int MEMOIRE_KIB = 65_536;
    /** Parallélisme (p) : ≥ 4. */
    public static final int PARALLELISME = 4;

    @Bean
    public Argon2 argon2() {
        // Sel ≥ 16 octets, hash 32 octets (défauts argon2-jvm)
        return Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }
}

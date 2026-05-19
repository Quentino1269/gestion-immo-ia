package com.immo.gestion.session.config;

import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration du bounded context session.
 * <ul>
 *   <li>{@link #dureeSession()} : 24 h fixes (D3, pas de rotation glissante).</li>
 *   <li>{@link #hashDummyAntiEnumeration} : hash argon2id pré-calculé au démarrage,
 *       utilisé pour exécuter une vérification factice quand l'email soumis n'existe pas
 *       (D7, anti-énumération : temps de réponse constant).</li>
 * </ul>
 */
@Configuration
public class SessionConfig {

    static final Duration DUREE_SESSION_V1 = Duration.ofHours(24);

    @Bean
    public Duration dureeSession() {
        return DUREE_SESSION_V1;
    }

    @Bean
    public HashMotDePasse hashDummyAntiEnumeration(HasheurMotDePasse hasheur) {
        return hasheur.hasher(new MotDePasseClair("dummy-anti-enumeration-v1"));
    }
}

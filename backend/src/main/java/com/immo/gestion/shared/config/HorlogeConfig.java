package com.immo.gestion.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Horloge applicative injectable. En production : {@link Clock#systemUTC()}.
 * Surchargée par les tests pour fixer le temps.
 */
@Configuration
public class HorlogeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

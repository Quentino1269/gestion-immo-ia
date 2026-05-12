package com.immo.gestion.utilisateur.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Versions actives des CGU et de la politique de confidentialité.
 * Cf. docs/slices/creation-utilisateur.md (D9) — historisées dans l'event d'inscription.
 */
@Component
public class ConsentementsActuels {

    private final String versionCgu;
    private final String versionConfidentialite;

    public ConsentementsActuels(
            @Value("${gestion-immo.consentements.cgu.version:cgu-2026-01-01}") String versionCgu,
            @Value("${gestion-immo.consentements.confidentialite.version:confidentialite-2026-01-01}") String versionConfidentialite
    ) {
        this.versionCgu = versionCgu;
        this.versionConfidentialite = versionConfidentialite;
    }

    public String versionCgu() {
        return versionCgu;
    }

    public String versionConfidentialite() {
        return versionConfidentialite;
    }
}

package com.immo.gestion.utilisateur.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Value object — adresse postale de domicile.
 * Invariants I-7 et I-8 du slice enrichissement-profil.
 */
public record Adresse(
        String numero,
        String voie,
        String complement,
        String codePostal,
        String commune,
        String paysIso
) {

    private static final Pattern NUMERO = Pattern.compile("^\\d+\\s*(bis|ter)?$", Pattern.CASE_INSENSITIVE);
    private static final int VOIE_MAX = 150;
    private static final int CODE_POSTAL_MAX = 10;
    private static final int COMMUNE_MAX = 100;
    private static final Set<String> PAYS_VALIDES =
            Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

    public Adresse {
        Objects.requireNonNull(numero, "numero requis");
        Objects.requireNonNull(voie, "voie requise");
        Objects.requireNonNull(codePostal, "codePostal requis");
        Objects.requireNonNull(commune, "commune requise");
        Objects.requireNonNull(paysIso, "paysIso requis");

        numero = numero.trim();
        voie = voie.trim();
        complement = complement != null && !complement.trim().isEmpty() ? complement.trim() : null;
        codePostal = codePostal.trim();
        commune = commune.trim();
        paysIso = paysIso.trim().toUpperCase();

        if (!NUMERO.matcher(numero).matches()) {
            throw new IllegalArgumentException("numero invalide (chiffres + éventuellement bis/ter)");
        }
        if (voie.isEmpty() || voie.length() > VOIE_MAX) {
            throw new IllegalArgumentException("voie invalide");
        }
        if (codePostal.isEmpty() || codePostal.length() > CODE_POSTAL_MAX) {
            throw new IllegalArgumentException("codePostal invalide");
        }
        if (commune.isEmpty() || commune.length() > COMMUNE_MAX) {
            throw new IllegalArgumentException("commune invalide");
        }
        if (!PAYS_VALIDES.contains(paysIso)) {
            throw new IllegalArgumentException("paysIso invalide (ISO 3166-1 alpha-2 attendu)");
        }
    }
}

package com.immo.gestion.rentabilite.domain;

/**
 * Cf. docs/slices/projection-rentabilite.md D6, D10, D11, D21.
 */
public enum RegimeFiscal {
    MICRO_FONCIER(false, false),
    REEL_FONCIER(false, true),
    MICRO_BIC(true, false),
    REEL_BIC(true, true);

    private final boolean meuble;
    private final boolean reel;

    RegimeFiscal(boolean meuble, boolean reel) {
        this.meuble = meuble;
        this.reel = reel;
    }

    /** Cohérence I-SIM-3 : nu → *_FONCIER, meublé → *_BIC. */
    public boolean compatibleAvecMeuble(boolean bienMeuble) {
        return this.meuble == bienMeuble;
    }

    public boolean estReel() {
        return reel;
    }
}

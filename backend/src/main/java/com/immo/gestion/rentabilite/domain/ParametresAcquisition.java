package com.immo.gestion.rentabilite.domain;

/** Cf. docs/slices/projection-rentabilite.md §7 "Acquisition". */
public record ParametresAcquisition(
        long prixAchatEnCentimes,
        long fraisNotaireEnCentimes,
        long fraisAgenceEnCentimes,
        long travauxAlAcquisitionEnCentimes,
        long fraisDossierBancaireEnCentimes
) {

    public ParametresAcquisition {
        // I-SIM-6
        if (prixAchatEnCentimes <= 0) {
            throw new IllegalArgumentException("prixAchatEnCentimes doit être > 0");
        }
        if (fraisNotaireEnCentimes < 0 || fraisAgenceEnCentimes < 0
                || travauxAlAcquisitionEnCentimes < 0 || fraisDossierBancaireEnCentimes < 0) {
            throw new IllegalArgumentException("les frais et travaux d'acquisition doivent être ≥ 0");
        }
    }

    /** Coût total d'acquisition (D1 §11), fixe, non indexé sur l'horizon. */
    public long coutTotalEnCentimes() {
        return prixAchatEnCentimes + fraisNotaireEnCentimes + fraisAgenceEnCentimes
                + travauxAlAcquisitionEnCentimes + fraisDossierBancaireEnCentimes;
    }
}

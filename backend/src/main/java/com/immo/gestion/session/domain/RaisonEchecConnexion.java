package com.immo.gestion.session.domain;

/**
 * Motif détaillé d'un échec de connexion, conservé dans
 * {@link TentativeDeConnexionEchouee} à des fins d'audit (D9).
 * <b>Ne sort jamais</b> côté API : la réponse client est strictement
 * identique quel que soit le motif (D7).
 */
public enum RaisonEchecConnexion {
    IDENTIFIANTS_INVALIDES,
    COMPTE_INACTIF
}

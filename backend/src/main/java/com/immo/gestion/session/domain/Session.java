package com.immo.gestion.session.domain;

import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Session — porte le cycle de vie d'une session unique (D8).
 * Record immuable : les transitions {@link #fermer(MotifFermeture, Instant)}
 * produisent une nouvelle instance qui est ensuite persistée.
 * <p>
 * Invariants : cf. docs/slices/authentification.md §11 (I-4 à I-7, I-9).
 */
public record Session(
        SessionId id,
        UtilisateurId utilisateurId,
        TokenSessionHash tokenHash,
        Instant ouverteLe,
        Instant expireA,
        EtatSession etat,
        MotifFermeture motifFermeture,
        Instant fermeeLe,
        String userAgent,
        String ipSource
) {

    private static final int LONGUEUR_USER_AGENT_MAX = 512;
    private static final int LONGUEUR_IP_MAX = 64;

    public Session {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(utilisateurId, "utilisateurId requis");
        Objects.requireNonNull(tokenHash, "tokenHash requis");
        Objects.requireNonNull(ouverteLe, "ouverteLe requis");
        Objects.requireNonNull(expireA, "expireA requis");
        Objects.requireNonNull(etat, "etat requis");

        if (!expireA.isAfter(ouverteLe)) {
            throw new IllegalArgumentException("expireA doit être postérieur à ouverteLe");
        }
        if (etat == EtatSession.FERMEE) {
            if (motifFermeture == null) {
                throw new IllegalArgumentException("motifFermeture requis quand etat=FERMEE");
            }
            if (fermeeLe == null) {
                throw new IllegalArgumentException("fermeeLe requis quand etat=FERMEE");
            }
        } else {
            if (motifFermeture != null || fermeeLe != null) {
                throw new IllegalArgumentException("motifFermeture/fermeeLe interdits quand etat=ACTIVE");
            }
        }

        userAgent = normaliserOptionnel(userAgent, LONGUEUR_USER_AGENT_MAX);
        ipSource = normaliserOptionnel(ipSource, LONGUEUR_IP_MAX);
    }

    /**
     * Ouvre une nouvelle session. {@code expireA = ouverteLe + dureeSession} (I-5).
     */
    public static Session ouvrir(
            UtilisateurId utilisateurId,
            TokenSessionHash tokenHash,
            Instant maintenant,
            Duration dureeSession,
            String userAgent,
            String ipSource
    ) {
        Objects.requireNonNull(maintenant, "maintenant requis");
        Objects.requireNonNull(dureeSession, "dureeSession requise");
        if (dureeSession.isZero() || dureeSession.isNegative()) {
            throw new IllegalArgumentException("dureeSession doit être strictement positive");
        }
        return new Session(
                SessionId.nouveau(),
                utilisateurId,
                tokenHash,
                maintenant,
                maintenant.plus(dureeSession),
                EtatSession.ACTIVE,
                null,
                null,
                userAgent,
                ipSource
        );
    }

    /**
     * Reconstruit l'aggregate par rejeu de son flux d'événements (Event Sourcing, MISSION §5).
     * Application inconditionnelle des faits déjà validés historiquement — ne revalide pas les
     * invariants métier (contrairement à {@link #fermer(MotifFermeture, Instant)} appelé en direct).
     */
    public static Session reconstruire(List<DomainEvent> evenements) {
        if (evenements.isEmpty()) {
            throw new IllegalStateException("Flux Session vide : impossible de reconstruire l'aggregate");
        }
        Session etat = null;
        for (DomainEvent evenement : evenements) {
            etat = appliquer(etat, evenement);
        }
        return etat;
    }

    private static Session appliquer(Session etat, DomainEvent evenement) {
        if (etat == null && !(evenement instanceof UtilisateurConnecte)) {
            throw new IllegalStateException(
                    "Flux Session corrompu : premier événement attendu UtilisateurConnecte, reçu " + evenement.getClass());
        }
        return switch (evenement) {
            case UtilisateurConnecte e -> new Session(
                    e.sessionId(), e.utilisateurId(), e.tokenHash(),
                    e.survenuLe(), e.expireA(), EtatSession.ACTIVE, null, null,
                    e.userAgent(), e.ipSource()
            );
            case UtilisateurDeconnecte e -> new Session(
                    etat.id(), etat.utilisateurId(), etat.tokenHash(), etat.ouverteLe(), etat.expireA(),
                    EtatSession.FERMEE, e.motif(), e.survenuLe(), etat.userAgent(), etat.ipSource()
            );
            default -> throw new IllegalStateException(
                    "Événement inattendu dans le flux Session : " + evenement.getClass());
        };
    }

    public boolean estActive(Instant maintenant) {
        Objects.requireNonNull(maintenant, "maintenant requis");
        return etat == EtatSession.ACTIVE && maintenant.isBefore(expireA);
    }

    /**
     * Ferme la session. Refuse de réactiver une session déjà fermée (I-7).
     */
    public Session fermer(MotifFermeture motif, Instant maintenant) {
        Objects.requireNonNull(motif, "motif requis");
        Objects.requireNonNull(maintenant, "maintenant requis");
        if (etat == EtatSession.FERMEE) {
            throw new IllegalStateException("session déjà fermée");
        }
        return new Session(
                id, utilisateurId, tokenHash, ouverteLe, expireA,
                EtatSession.FERMEE, motif, maintenant, userAgent, ipSource
        );
    }

    public Optional<String> userAgentOpt() {
        return Optional.ofNullable(userAgent);
    }

    public Optional<String> ipSourceOpt() {
        return Optional.ofNullable(ipSource);
    }

    private static String normaliserOptionnel(String valeur, int longueurMax) {
        if (valeur == null) {
            return null;
        }
        String trim = valeur.trim();
        if (trim.isEmpty()) {
            return null;
        }
        if (trim.length() > longueurMax) {
            return trim.substring(0, longueurMax);
        }
        return trim;
    }
}

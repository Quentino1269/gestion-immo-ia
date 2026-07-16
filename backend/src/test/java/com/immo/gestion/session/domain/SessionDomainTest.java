package com.immo.gestion.session.domain;

import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionDomainTest {

    private static final Instant T0 = Instant.parse("2026-05-20T10:00:00Z");
    private static final Duration VINGT_QUATRE_H = Duration.ofHours(24);
    private static final UtilisateurId UID = new UtilisateurId(UUID.randomUUID());
    private static final TokenSessionHash HASH = new TokenSessionHash(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    );

    @Test
    void ouvrir_pose_expireA_egal_maintenant_plus_duree() {
        Session s = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, "ua", "1.2.3.4");

        assertThat(s.ouverteLe()).isEqualTo(T0);
        assertThat(s.expireA()).isEqualTo(T0.plus(VINGT_QUATRE_H));
        assertThat(s.etat()).isEqualTo(EtatSession.ACTIVE);
        assertThat(s.motifFermeture()).isNull();
        assertThat(s.fermeeLe()).isNull();
        assertThat(s.userAgentOpt()).contains("ua");
        assertThat(s.ipSourceOpt()).contains("1.2.3.4");
    }

    @Test
    void ouvrir_avec_duree_nulle_lance_exception() {
        assertThatThrownBy(() -> Session.ouvrir(UID, HASH, T0, Duration.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ouvrir_avec_duree_negative_lance_exception() {
        assertThatThrownBy(() -> Session.ouvrir(UID, HASH, T0, Duration.ofSeconds(-1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void user_agent_et_ip_vides_sont_normalises_en_null() {
        Session s = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, "   ", "");

        assertThat(s.userAgentOpt()).isEmpty();
        assertThat(s.ipSourceOpt()).isEmpty();
    }

    @Test
    void user_agent_trop_long_est_tronque() {
        String long512Plus = "x".repeat(600);
        Session s = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, long512Plus, null);

        assertThat(s.userAgent()).hasSize(512);
    }

    @Test
    void estActive_renvoie_true_quand_active_et_avant_expiration() {
        Session s = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, null, null);

        assertThat(s.estActive(T0)).isTrue();
        assertThat(s.estActive(T0.plus(Duration.ofHours(23)))).isTrue();
        assertThat(s.estActive(T0.plus(VINGT_QUATRE_H))).isFalse(); // borne stricte
        assertThat(s.estActive(T0.plus(Duration.ofHours(25)))).isFalse();
    }

    @Test
    void fermer_transitionne_vers_FERMEE_avec_motif_et_horodatage() {
        Session s = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, null, null);
        Instant tFermeture = T0.plus(Duration.ofHours(2));

        Session fermee = s.fermer(MotifFermeture.VOLONTAIRE, tFermeture);

        assertThat(fermee.etat()).isEqualTo(EtatSession.FERMEE);
        assertThat(fermee.motifFermeture()).isEqualTo(MotifFermeture.VOLONTAIRE);
        assertThat(fermee.fermeeLe()).isEqualTo(tFermeture);
        assertThat(fermee.estActive(tFermeture)).isFalse();
    }

    @Test
    void fermer_une_session_deja_fermee_lance_illegal_state() {
        Session fermee = Session.ouvrir(UID, HASH, T0, VINGT_QUATRE_H, null, null)
                .fermer(MotifFermeture.VOLONTAIRE, T0.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> fermee.fermer(MotifFermeture.REVOQUEE, T0.plus(Duration.ofMinutes(10))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void construction_directe_avec_etat_ferme_sans_motif_lance_exception() {
        assertThatThrownBy(() -> new Session(
                SessionId.nouveau(), UID, HASH, T0, T0.plus(VINGT_QUATRE_H),
                EtatSession.FERMEE, null, T0.plus(Duration.ofMinutes(1)), null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construction_directe_avec_etat_actif_et_motif_lance_exception() {
        assertThatThrownBy(() -> new Session(
                SessionId.nouveau(), UID, HASH, T0, T0.plus(VINGT_QUATRE_H),
                EtatSession.ACTIVE, MotifFermeture.VOLONTAIRE, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construction_directe_avec_expireA_anterieur_a_ouverteLe_lance_exception() {
        assertThatThrownBy(() -> new Session(
                SessionId.nouveau(), UID, HASH, T0, T0.minusSeconds(1),
                EtatSession.ACTIVE, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenSessionHash_exige_64_caracteres_hex_lowercase() {
        assertThatThrownBy(() -> new TokenSessionHash("trop court"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenSessionHash("0".repeat(63)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenSessionHash("ABCDEF".repeat(10) + "abcd"))
                .isInstanceOf(IllegalArgumentException.class); // majuscules
        assertThatThrownBy(() -> new TokenSessionHash("zz".repeat(32)))
                .isInstanceOf(IllegalArgumentException.class); // hors [0-9a-f]
    }

    @Test
    void tokenSession_vide_lance_exception() {
        assertThatThrownBy(() -> new TokenSession(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TokenSession("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenSession_toString_masque_la_valeur() {
        TokenSession t = new TokenSession("super-secret-token");

        assertThat(t.toString()).doesNotContain("super-secret-token");
        assertThat(t.toString()).contains("masqué");
    }

    // -------------------- Reconstruction par rejeu (Event Sourcing) --------------------

    @Test
    void reconstruire_avec_UtilisateurConnecte_seul_recree_une_session_active() {
        UtilisateurConnecte evenement = new UtilisateurConnecte(
                SessionId.nouveau(), UID, HASH, T0.plus(VINGT_QUATRE_H), "ua", "1.2.3.4", T0
        );

        Session s = Session.reconstruire(List.of(evenement));

        assertThat(s.id()).isEqualTo(evenement.sessionId());
        assertThat(s.utilisateurId()).isEqualTo(UID);
        assertThat(s.tokenHash()).isEqualTo(HASH);
        assertThat(s.ouverteLe()).isEqualTo(T0);
        assertThat(s.expireA()).isEqualTo(T0.plus(VINGT_QUATRE_H));
        assertThat(s.etat()).isEqualTo(EtatSession.ACTIVE);
    }

    @Test
    void reconstruire_avec_connexion_puis_deconnexion_recree_une_session_fermee() {
        SessionId id = SessionId.nouveau();
        Instant tFermeture = T0.plus(Duration.ofHours(2));
        List<DomainEvent> evenements = List.of(
                new UtilisateurConnecte(id, UID, HASH, T0.plus(VINGT_QUATRE_H), null, null, T0),
                new UtilisateurDeconnecte(id, UID, MotifFermeture.VOLONTAIRE, tFermeture)
        );

        Session s = Session.reconstruire(evenements);

        assertThat(s.etat()).isEqualTo(EtatSession.FERMEE);
        assertThat(s.motifFermeture()).isEqualTo(MotifFermeture.VOLONTAIRE);
        assertThat(s.fermeeLe()).isEqualTo(tFermeture);
    }

    @Test
    void reconstruire_ne_revalide_pas_les_invariants_metier_meme_sur_un_flux_incoherent() {
        // Deux UtilisateurDeconnecte consécutifs : ne devrait normalement jamais arriver via
        // SessionService (verrou optimiste), mais le rejeu doit rester une application
        // inconditionnelle des faits, pas une revalidation (contrairement à fermer() appelé en direct).
        SessionId id = SessionId.nouveau();
        List<DomainEvent> evenements = List.of(
                new UtilisateurConnecte(id, UID, HASH, T0.plus(VINGT_QUATRE_H), null, null, T0),
                new UtilisateurDeconnecte(id, UID, MotifFermeture.VOLONTAIRE, T0.plus(Duration.ofHours(1))),
                new UtilisateurDeconnecte(id, UID, MotifFermeture.EXPIRATION, T0.plus(Duration.ofHours(2)))
        );

        Session s = Session.reconstruire(evenements);

        assertThat(s.etat()).isEqualTo(EtatSession.FERMEE);
        assertThat(s.motifFermeture()).isEqualTo(MotifFermeture.EXPIRATION);
    }

    @Test
    void reconstruire_avec_flux_vide_leve_une_exception() {
        assertThatThrownBy(() -> Session.reconstruire(List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstruire_avec_premier_evenement_inattendu_leve_une_exception() {
        List<DomainEvent> evenements = List.of(
                new UtilisateurDeconnecte(SessionId.nouveau(), UID, MotifFermeture.VOLONTAIRE, T0)
        );

        assertThatThrownBy(() -> Session.reconstruire(evenements))
                .isInstanceOf(IllegalStateException.class);
    }
}

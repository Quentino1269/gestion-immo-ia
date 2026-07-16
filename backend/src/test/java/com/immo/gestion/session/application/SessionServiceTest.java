package com.immo.gestion.session.application;

import com.immo.gestion.session.domain.MotifFermeture;
import com.immo.gestion.session.domain.RaisonEchecConnexion;
import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TentativeDeConnexionEchouee;
import com.immo.gestion.session.domain.TokenSession;
import com.immo.gestion.session.domain.TokenSessionGenere;
import com.immo.gestion.session.domain.TokenSessionHash;
import com.immo.gestion.session.domain.UtilisateurConnecte;
import com.immo.gestion.session.domain.UtilisateurDeconnecte;
import com.immo.gestion.session.domain.port.in.IdentifiantsInvalidesException;
import com.immo.gestion.session.domain.port.in.SeConnecterCommand;
import com.immo.gestion.session.domain.port.in.SeConnecterResultat;
import com.immo.gestion.session.domain.port.in.SeDeconnecterCommand;
import com.immo.gestion.session.domain.port.in.SessionInvalideException;
import com.immo.gestion.session.domain.port.out.GenerateurTokenSession;
import com.immo.gestion.session.domain.port.out.InfosAuthentification;
import com.immo.gestion.session.domain.port.out.RepertoireAuthentification;
import com.immo.gestion.session.domain.port.out.SessionRepository;
import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseSoumis;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private static final Instant T = Instant.parse("2026-05-20T10:00:00Z");
    private static final Duration DUREE = Duration.ofHours(24);
    private static final UtilisateurId UID = new UtilisateurId(UUID.randomUUID());
    private static final HashMotDePasse HASH_REEL = new HashMotDePasse(
            "$argon2id$v=19$m=65536,t=3,p=4$AAAAAAAAAAAAAAAA$BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    );
    private static final HashMotDePasse HASH_DUMMY = new HashMotDePasse(
            "$argon2id$v=19$m=65536,t=3,p=4$CCCCCCCCCCCCCCCC$DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"
    );
    private static final TokenSessionGenere TOKEN_GENERE = new TokenSessionGenere(
            new TokenSession("clair-base64url-ok"),
            new TokenSessionHash("0".repeat(64))
    );

    private RepertoireAuthentification repertoire;
    private HasheurMotDePasse hasheur;
    private GenerateurTokenSession generateurToken;
    private SessionRepository sessions;
    private ApplicationEventPublisher publisher;
    private SessionService service;

    @BeforeEach
    void setUp() {
        repertoire = mock(RepertoireAuthentification.class);
        hasheur = mock(HasheurMotDePasse.class);
        generateurToken = mock(GenerateurTokenSession.class);
        sessions = mock(SessionRepository.class);
        publisher = mock(ApplicationEventPublisher.class);

        when(generateurToken.genererNouveau()).thenReturn(TOKEN_GENERE);

        service = new SessionService(
                repertoire, hasheur, HASH_DUMMY, generateurToken,
                sessions, publisher, Clock.fixed(T, ZoneOffset.UTC), DUREE
        );
    }

    private static SeConnecterCommand commandeAvec(String email, String motDePasse) {
        return new SeConnecterCommand(
                email, new MotDePasseSoumis(motDePasse), "ua", "1.2.3.4"
        );
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DomainEvent>> eventsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    // -------------------- Golden path --------------------

    @Test
    void seConnecter_avec_credentials_valides_persiste_session_et_emet_event() {
        when(repertoire.chercherParEmail(new Email("foo@bar.com")))
                .thenReturn(Optional.of(new InfosAuthentification(UID, HASH_REEL, StatutCompte.ACTIF)));
        when(hasheur.verifierSoumission(eq(HASH_REEL), any())).thenReturn(true);

        SeConnecterResultat resultat = service.seConnecter(commandeAvec("Foo@Bar.com", "ChouchouEtLoulou123"));

        assertThat(resultat.tokenClair()).isSameAs(TOKEN_GENERE.clair());
        assertThat(resultat.expireA()).isEqualTo(T.plus(DUREE));

        ArgumentCaptor<SessionId> idCaptor = ArgumentCaptor.forClass(SessionId.class);
        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(sessions).enregistrer(idCaptor.capture(), eq(0L), evenementsCaptor.capture());

        assertThat(resultat.sessionId()).isEqualTo(idCaptor.getValue());
        assertThat(evenementsCaptor.getValue()).hasSize(1);
        UtilisateurConnecte evenement = (UtilisateurConnecte) evenementsCaptor.getValue().get(0);
        assertThat(evenement.sessionId()).isEqualTo(idCaptor.getValue());
        assertThat(evenement.utilisateurId()).isEqualTo(UID);
        assertThat(evenement.tokenHash()).isEqualTo(TOKEN_GENERE.hash());
        assertThat(evenement.expireA()).isEqualTo(T.plus(DUREE));
    }

    // -------------------- Anti-énumération --------------------

    @Test
    void seConnecter_email_inconnu_execute_dummy_hash_emet_event_et_echoue() {
        when(repertoire.chercherParEmail(any())).thenReturn(Optional.empty());

        MotDePasseSoumis soumission = new MotDePasseSoumis("ChouchouEtLoulou123");
        char[] avant = soumission.caracteres();

        assertThatThrownBy(() -> service.seConnecter(new SeConnecterCommand(
                "inconnu@example.com", soumission, "ua", "1.2.3.4"
        ))).isInstanceOf(IdentifiantsInvalidesException.class);

        // Dummy hash exécuté exactement une fois sur le chemin "email inconnu"
        verify(hasheur, times(1)).verifierSoumission(eq(HASH_DUMMY), any());

        // Aucune session persistée, event d'audit avec raison IDENTIFIANTS_INVALIDES
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
        ArgumentCaptor<TentativeDeConnexionEchouee> evt =
                ArgumentCaptor.forClass(TentativeDeConnexionEchouee.class);
        verify(publisher).publishEvent(evt.capture());
        assertThat(evt.getValue().raison()).isEqualTo(RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
        assertThat(evt.getValue().emailSoumis()).isEqualTo("inconnu@example.com");
        assertThat(evt.getValue().survenuLe()).isEqualTo(T);

        // Le mot de passe soumis est effacé en mémoire après la tentative
        assertThat(new String(avant)).isEqualTo("\0".repeat("ChouchouEtLoulou123".length()));
    }

    @Test
    void seConnecter_email_mal_forme_execute_aussi_dummy_hash_pour_timing_constant() {
        // Pas de lookup, l'email est rejeté côté VO Email
        assertThatThrownBy(() -> service.seConnecter(commandeAvec("pas-un-email", "ChouchouEtLoulou123")))
                .isInstanceOf(IdentifiantsInvalidesException.class);

        // Le dummy hash doit être exécuté pour ne pas trahir le format invalide par un temps de réponse rapide
        verify(hasheur, times(1)).verifierSoumission(eq(HASH_DUMMY), any());
        verify(publisher, times(1)).publishEvent(any(TentativeDeConnexionEchouee.class));
    }

    @Test
    void seConnecter_mot_de_passe_invalide_emet_event_et_echoue() {
        when(repertoire.chercherParEmail(any()))
                .thenReturn(Optional.of(new InfosAuthentification(UID, HASH_REEL, StatutCompte.ACTIF)));
        when(hasheur.verifierSoumission(eq(HASH_REEL), any())).thenReturn(false);

        assertThatThrownBy(() -> service.seConnecter(commandeAvec("foo@bar.com", "ChouchouEtLoulou123")))
                .isInstanceOf(IdentifiantsInvalidesException.class);

        // Pas de dummy hash sur ce chemin (email connu), juste la vérification réelle
        verify(hasheur, times(1)).verifierSoumission(any(), any());
        verify(hasheur, never()).verifierSoumission(eq(HASH_DUMMY), any());

        ArgumentCaptor<TentativeDeConnexionEchouee> evt =
                ArgumentCaptor.forClass(TentativeDeConnexionEchouee.class);
        verify(publisher).publishEvent(evt.capture());
        assertThat(evt.getValue().raison()).isEqualTo(RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
    }

    @Test
    void seConnecter_compte_inactif_emet_event_COMPTE_INACTIF_mais_meme_exception_API() {
        when(repertoire.chercherParEmail(any()))
                .thenReturn(Optional.of(new InfosAuthentification(UID, HASH_REEL, StatutCompte.SUSPENDU)));
        when(hasheur.verifierSoumission(eq(HASH_REEL), any())).thenReturn(true);

        assertThatThrownBy(() -> service.seConnecter(commandeAvec("foo@bar.com", "ChouchouEtLoulou123")))
                .isInstanceOf(IdentifiantsInvalidesException.class); // même exception côté API

        ArgumentCaptor<TentativeDeConnexionEchouee> evt =
                ArgumentCaptor.forClass(TentativeDeConnexionEchouee.class);
        verify(publisher).publishEvent(evt.capture());
        assertThat(evt.getValue().raison()).isEqualTo(RaisonEchecConnexion.COMPTE_INACTIF);
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
    }

    @Test
    void seConnecter_normalise_email_avant_lookup() {
        when(repertoire.chercherParEmail(any())).thenReturn(Optional.empty());

        try {
            service.seConnecter(commandeAvec("  Foo@Bar.COM  ", "ChouchouEtLoulou123"));
        } catch (IdentifiantsInvalidesException ignored) {
            // attendu
        }

        verify(repertoire).chercherParEmail(new Email("foo@bar.com"));
    }

    @Test
    void seConnecter_efface_le_mot_de_passe_meme_en_cas_de_succes() {
        when(repertoire.chercherParEmail(any()))
                .thenReturn(Optional.of(new InfosAuthentification(UID, HASH_REEL, StatutCompte.ACTIF)));
        when(hasheur.verifierSoumission(eq(HASH_REEL), any())).thenReturn(true);

        MotDePasseSoumis soumission = new MotDePasseSoumis("ChouchouEtLoulou123");
        char[] ref = soumission.caracteres();

        service.seConnecter(new SeConnecterCommand("foo@bar.com", soumission, null, null));

        assertThat(new String(ref)).isEqualTo("\0".repeat("ChouchouEtLoulou123".length()));
    }

    // -------------------- Déconnexion --------------------

    @Test
    void seDeconnecter_session_active_de_l_appelant_la_ferme_et_emet_event() {
        Session active = Session.ouvrir(UID, TOKEN_GENERE.hash(), T.minus(Duration.ofHours(1)), DUREE, null, null);
        when(sessions.chargerParId(active.id())).thenReturn(Optional.of(new EtatCharge<>(active, 1L)));

        service.seDeconnecter(new SeDeconnecterCommand(active.id(), UID));

        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(sessions).enregistrer(eq(active.id()), eq(1L), evenementsCaptor.capture());

        assertThat(evenementsCaptor.getValue()).hasSize(1);
        UtilisateurDeconnecte evenement = (UtilisateurDeconnecte) evenementsCaptor.getValue().get(0);
        assertThat(evenement.motif()).isEqualTo(MotifFermeture.VOLONTAIRE);
        assertThat(evenement.sessionId()).isEqualTo(active.id());
        assertThat(evenement.survenuLe()).isEqualTo(T);
    }

    @Test
    void seDeconnecter_session_inexistante_lance_SessionInvalide() {
        SessionId sid = SessionId.nouveau();
        when(sessions.chargerParId(sid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.seDeconnecter(new SeDeconnecterCommand(sid, UID)))
                .isInstanceOf(SessionInvalideException.class);
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void seDeconnecter_session_d_un_autre_utilisateur_est_refusee() {
        UtilisateurId proprietaire = new UtilisateurId(UUID.randomUUID());
        UtilisateurId attaquant = new UtilisateurId(UUID.randomUUID());
        Session sessionAutrui = Session.ouvrir(proprietaire, TOKEN_GENERE.hash(), T, DUREE, null, null);
        when(sessions.chargerParId(sessionAutrui.id())).thenReturn(Optional.of(new EtatCharge<>(sessionAutrui, 1L)));

        assertThatThrownBy(() -> service.seDeconnecter(new SeDeconnecterCommand(sessionAutrui.id(), attaquant)))
                .isInstanceOf(SessionInvalideException.class);
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
    }

    @Test
    void seDeconnecter_session_deja_fermee_est_refusee() {
        Session deja = Session.ouvrir(UID, TOKEN_GENERE.hash(), T.minus(Duration.ofHours(2)), DUREE, null, null)
                .fermer(MotifFermeture.VOLONTAIRE, T.minus(Duration.ofHours(1)));
        when(sessions.chargerParId(deja.id())).thenReturn(Optional.of(new EtatCharge<>(deja, 2L)));

        assertThatThrownBy(() -> service.seDeconnecter(new SeDeconnecterCommand(deja.id(), UID)))
                .isInstanceOf(SessionInvalideException.class);
        verify(sessions, never()).enregistrer(any(), anyLong(), any());
    }
}

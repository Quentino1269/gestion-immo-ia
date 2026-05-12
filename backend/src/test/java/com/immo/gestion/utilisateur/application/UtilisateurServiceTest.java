package com.immo.gestion.utilisateur.application;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;
import com.immo.gestion.utilisateur.config.ConsentementsActuels;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import com.immo.gestion.utilisateur.domain.port.in.ConsentementsNonAcceptesException;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurCommand;
import com.immo.gestion.utilisateur.domain.port.in.EmailDejaUtiliseException;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilisateurServiceTest {

    private static final Instant T = Instant.parse("2026-05-13T10:00:00Z");

    private UtilisateurRepository repository;
    private HasheurMotDePasse hasheur;
    private ApplicationEventPublisher publisher;
    private List<Object> eventsCaptures;
    private UtilisateurService service;

    @BeforeEach
    void setUp() {
        repository = mock(UtilisateurRepository.class);
        hasheur = mock(HasheurMotDePasse.class);
        publisher = mock(ApplicationEventPublisher.class);
        eventsCaptures = new ArrayList<>();

        when(repository.existeParEmail(any())).thenReturn(false);
        when(hasheur.hasher(any())).thenReturn(new HashMotDePasse("$argon2id$v=19$m=65536,t=3,p=4$AAAAAAAAAAAAAAAA$BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"));

        ConsentementsActuels consentements = new ConsentementsActuels("cgu-1", "conf-1");
        Clock fixe = Clock.fixed(T, ZoneOffset.UTC);

        service = new UtilisateurService(repository, hasheur, consentements, publisher, fixe);
    }

    private CreerUtilisateurCommand valide() {
        return new CreerUtilisateurCommand(
                "Foo@Bar.com",
                "MotDePasseSolide!",
                "Dupont",
                "Jean",
                "+33612345678",
                true,
                true
        );
    }

    @Test
    void creer_avec_donnees_valides_persiste_et_emet_event() {
        UtilisateurId id = service.creer(valide());

        assertThat(id).isNotNull();
        verify(repository).enregistrer(any(Utilisateur.class));
        verify(publisher).publishEvent(any(UtilisateurInscrit.class));
    }

    @Test
    void creer_normalise_email_avant_lookup_et_persistance() {
        service.creer(valide());

        verify(repository).existeParEmail(new Email("foo@bar.com"));
    }

    @Test
    void creer_sans_cgu_lance_exception_et_ne_persiste_rien() {
        CreerUtilisateurCommand sansCgu = new CreerUtilisateurCommand(
                "foo@bar.com", "MotDePasseSolide!", "Dupont", "Jean",
                null, false, true
        );

        assertThatThrownBy(() -> service.creer(sansCgu))
                .isInstanceOf(ConsentementsNonAcceptesException.class);
        verify(repository, never()).enregistrer(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void creer_sans_confidentialite_lance_exception() {
        CreerUtilisateurCommand sansConf = new CreerUtilisateurCommand(
                "foo@bar.com", "MotDePasseSolide!", "Dupont", "Jean",
                null, true, false
        );

        assertThatThrownBy(() -> service.creer(sansConf))
                .isInstanceOf(ConsentementsNonAcceptesException.class);
    }

    @Test
    void creer_avec_email_existant_lance_exception_anti_enumeration() {
        when(repository.existeParEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> service.creer(valide()))
                .isInstanceOf(EmailDejaUtiliseException.class);
        verify(repository, never()).enregistrer(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void creer_avec_email_malforme_lance_illegal_argument() {
        CreerUtilisateurCommand mauvais = new CreerUtilisateurCommand(
                "pas-un-email", "MotDePasseSolide!", "Dupont", "Jean",
                null, true, true
        );

        assertThatThrownBy(() -> service.creer(mauvais))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creer_avec_motdepasse_trop_court_lance_illegal_argument() {
        CreerUtilisateurCommand court = new CreerUtilisateurCommand(
                "foo@bar.com", "court", "Dupont", "Jean",
                null, true, true
        );

        assertThatThrownBy(() -> service.creer(court))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creer_avec_telephone_non_e164_lance_illegal_argument() {
        CreerUtilisateurCommand bad = new CreerUtilisateurCommand(
                "foo@bar.com", "MotDePasseSolide!", "Dupont", "Jean",
                "0612345678", // pas E.164
                true, true
        );

        assertThatThrownBy(() -> service.creer(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creer_avec_telephone_null_persiste_sans_telephone() {
        CreerUtilisateurCommand sansTel = new CreerUtilisateurCommand(
                "foo@bar.com", "MotDePasseSolide!", "Dupont", "Jean",
                null, true, true
        );

        UtilisateurId id = service.creer(sansTel);

        assertThat(id).isNotNull();
        verify(repository).enregistrer(any());
    }

    @Test
    void creer_persiste_statut_actif_et_versions_consentements() {
        when(repository.chargerParId(any())).thenAnswer(invocation -> {
            // capturer ce qui a été persisté via le repo mock
            return Optional.empty();
        });

        var captor = org.mockito.ArgumentCaptor.forClass(Utilisateur.class);
        service.creer(valide());
        verify(repository).enregistrer(captor.capture());

        Utilisateur sauvegarde = captor.getValue();
        assertThat(sauvegarde.statut()).isEqualTo(StatutCompte.ACTIF);
        assertThat(sauvegarde.versionCgu()).isEqualTo("cgu-1");
        assertThat(sauvegarde.versionConfidentialite()).isEqualTo("conf-1");
        assertThat(sauvegarde.cguAccepteesLe()).isEqualTo(T);
        assertThat(sauvegarde.confidentialiteAccepteeLe()).isEqualTo(T);
        assertThat(sauvegarde.inscritLe()).isEqualTo(T);
    }
}

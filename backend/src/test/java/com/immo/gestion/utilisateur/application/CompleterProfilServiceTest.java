package com.immo.gestion.utilisateur.application;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.config.ConsentementsActuels;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.AdresseDomicileRenseignee;
import com.immo.gestion.utilisateur.domain.Civilite;
import com.immo.gestion.utilisateur.domain.CiviliteRenseignee;
import com.immo.gestion.utilisateur.domain.DonneesNaissanceRenseignees;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateur;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateurComplete;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.StatutProfil;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.in.CompleterMonProfilCivilCommand;
import com.immo.gestion.utilisateur.domain.port.in.ModificationProfilRefuseeException;
import com.immo.gestion.utilisateur.domain.port.in.UtilisateurNonTrouveException;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurQueryRepository;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleterProfilServiceTest {

    private static final Instant T = Instant.parse("2026-06-01T10:00:00Z");
    private static final String HASH = "$argon2id$v=19$m=65536,t=3,p=4$AAAAAAAAAAAAAAAA$BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private UtilisateurRepository repository;
    private UtilisateurQueryRepository queryRepository;
    private UtilisateurService service;
    private UtilisateurId uid;

    @BeforeEach
    void setUp() {
        repository = mock(UtilisateurRepository.class);
        queryRepository = mock(UtilisateurQueryRepository.class);
        HasheurMotDePasse hasheur = mock(HasheurMotDePasse.class);
        uid = UtilisateurId.nouveau();

        service = new UtilisateurService(
                repository, queryRepository, hasheur, new ConsentementsActuels("cgu-1", "conf-1"),
                Clock.fixed(T, ZoneOffset.UTC)
        );

        when(repository.chargerParId(uid)).thenReturn(Optional.of(new EtatCharge<>(utilisateurMinimal(uid), 1L)));
        when(queryRepository.chargerParId(uid)).thenReturn(Optional.of(utilisateurMinimal(uid)));
    }

    private Utilisateur utilisateurMinimal(UtilisateurId id) {
        return new Utilisateur(
                id,
                new Email("alice@example.com"),
                new HashMotDePasse(HASH),
                "Dupont", "Alice", null,
                StatutCompte.ACTIF, "cgu-1", T, "conf-1", T, T,
                null, null, null, null, null, null,
                StatutProfil.MINIMAL, null
        );
    }

    private CompleterMonProfilCivilCommand commandeComplete() {
        return new CompleterMonProfilCivilCommand(
                uid,
                Civilite.MADAME,
                LocalDate.of(1990, 6, 1),
                "Paris", "FR", "FR",
                new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR"),
                "+33612345678"
        );
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<DomainEvent>> eventsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Test
    void completer_profil_complet_retourne_statut_COMPLET() {
        ProfilUtilisateur profil = service.completer(commandeComplete());

        assertThat(profil.statutProfil()).isEqualTo(StatutProfil.COMPLET);
        assertThat(profil.civilite()).isEqualTo(Civilite.MADAME);
        assertThat(profil.champsManquantsPourBail()).isEmpty();
    }

    @Test
    void completer_persiste_les_evenements_avec_la_version_attendue() {
        service.completer(commandeComplete());

        verify(repository).enregistrer(eq(uid), eq(1L), any());
    }

    @Test
    void completer_transmet_les_evenements_domaine_au_repository() {
        service.completer(commandeComplete());

        ArgumentCaptor<List<DomainEvent>> evenementsCaptor = eventsCaptor();
        verify(repository).enregistrer(eq(uid), eq(1L), evenementsCaptor.capture());

        List<DomainEvent> evenements = evenementsCaptor.getValue();
        assertThat(evenements).hasAtLeastOneElementOfType(CiviliteRenseignee.class);
        assertThat(evenements).hasAtLeastOneElementOfType(DonneesNaissanceRenseignees.class);
        assertThat(evenements).hasAtLeastOneElementOfType(AdresseDomicileRenseignee.class);
        assertThat(evenements).hasAtLeastOneElementOfType(ProfilUtilisateurComplete.class);
    }

    @Test
    void completer_utilisateur_inexistant_leve_UtilisateurNonTrouveException() {
        when(repository.chargerParId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completer(commandeComplete()))
                .isInstanceOf(UtilisateurNonTrouveException.class);
    }

    @Test
    void completer_avec_modification_leve_ModificationProfilRefuseeException() {
        // Première complétion avec MADAME
        service.completer(commandeComplete());

        // Simuler que le repo retourne l'utilisateur mis à jour
        Utilisateur apresComplétion = utilisateurMinimal(uid).completerProfil(commandeComplete(), T).misAJour();
        when(repository.chargerParId(uid)).thenReturn(Optional.of(new EtatCharge<>(apresComplétion, 5L)));

        // Tenter de modifier la civilité
        CompleterMonProfilCivilCommand modification = new CompleterMonProfilCivilCommand(
                uid, Civilite.MONSIEUR, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.completer(modification))
                .isInstanceOf(ModificationProfilRefuseeException.class);
    }

    @Test
    void obtenir_profil_retourne_lecture_du_profil_courant() {
        ProfilUtilisateur profil = service.obtenir(uid);

        assertThat(profil.utilisateurId()).isEqualTo(uid);
        assertThat(profil.statutProfil()).isEqualTo(StatutProfil.MINIMAL);
        assertThat(profil.champsManquantsPourBail()).containsExactly("dateNaissance", "adresseDomicile");
    }

    @Test
    void obtenir_profil_inexistant_leve_exception() {
        when(queryRepository.chargerParId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenir(uid))
                .isInstanceOf(UtilisateurNonTrouveException.class);
    }
}

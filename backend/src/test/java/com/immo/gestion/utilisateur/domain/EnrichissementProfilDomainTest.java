package com.immo.gestion.utilisateur.domain;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.domain.port.in.CompleterMonProfilCivilCommand;
import com.immo.gestion.utilisateur.domain.port.in.ModificationProfilRefuseeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrichissementProfilDomainTest {

    private static final Instant T = Instant.parse("2026-06-01T10:00:00Z");
    private static final String HASH = "$argon2id$v=19$m=65536,t=3,p=4$AAAAAAAAAAAAAAAA$BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private Utilisateur utilisateurMinimal() {
        return new Utilisateur(
                UtilisateurId.nouveau(),
                new Email("alice@example.com"),
                new HashMotDePasse(HASH),
                "Dupont", "Alice", null,
                StatutCompte.ACTIF,
                "cgu-1", T, "conf-1", T, T,
                null, null, null, null, null, null,
                StatutProfil.MINIMAL, null
        );
    }

    private Adresse adresseValide() {
        return new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "FR");
    }

    private LocalDate dateNaissanceValide() {
        return LocalDate.of(1990, 6, 1); // 36 ans au moment T
    }

    // =========================================================
    // Chemins nominaux
    // =========================================================

    @Test
    void completer_profil_civil_complet_emet_tous_les_evenements_et_bascule_statut() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), Civilite.MADAME, dateNaissanceValide(),
                "Paris", "FR", "FR",
                adresseValide(), "+33612345678"
        );

        ResultatCompletionProfil resultat = u.completerProfil(cmd, T);

        assertThat(resultat.misAJour().statutProfil()).isEqualTo(StatutProfil.COMPLET);
        assertThat(resultat.misAJour().civilite()).isEqualTo(Civilite.MADAME);
        assertThat(resultat.misAJour().dateNaissance()).isEqualTo(dateNaissanceValide());
        assertThat(resultat.misAJour().lieuNaissanceVille()).isEqualTo("Paris");
        assertThat(resultat.misAJour().lieuNaissancePaysIso()).isEqualTo("FR");
        assertThat(resultat.misAJour().nationaliteIso()).isEqualTo("FR");
        assertThat(resultat.misAJour().adresseDomicile()).isEqualTo(adresseValide());
        assertThat(resultat.misAJour().telephone()).isEqualTo("+33612345678");

        List<Object> events = resultat.evenements();
        assertThat(events).hasSize(5);
        assertThat(events.get(0)).isInstanceOf(CiviliteRenseignee.class);
        assertThat(events.get(1)).isInstanceOf(DonneesNaissanceRenseignees.class);
        assertThat(events.get(2)).isInstanceOf(AdresseDomicileRenseignee.class);
        assertThat(events.get(3)).isInstanceOf(TelephoneRenseigne.class);
        assertThat(events.get(4)).isInstanceOf(ProfilUtilisateurComplete.class);
    }

    @Test
    void completer_avec_donnees_minimales_D8_seules_bascule_statut_COMPLET() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), null, dateNaissanceValide(),
                "Lyon", "FR", null,
                adresseValide(), null
        );

        ResultatCompletionProfil resultat = u.completerProfil(cmd, T);

        assertThat(resultat.misAJour().statutProfil()).isEqualTo(StatutProfil.COMPLET);
        List<Object> events = resultat.evenements();
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(DonneesNaissanceRenseignees.class);
        assertThat(events.get(1)).isInstanceOf(AdresseDomicileRenseignee.class);
        assertThat(events.get(2)).isInstanceOf(ProfilUtilisateurComplete.class);
    }

    @Test
    void completer_partiellement_ne_bascule_pas_statut() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), Civilite.MONSIEUR, null, null, null, null, null, null
        );

        ResultatCompletionProfil resultat = u.completerProfil(cmd, T);

        assertThat(resultat.misAJour().statutProfil()).isEqualTo(StatutProfil.MINIMAL);
        assertThat(resultat.evenements()).hasSize(1);
        assertThat(resultat.evenements().get(0)).isInstanceOf(CiviliteRenseignee.class);
    }

    @Test
    void completer_en_deux_passes_bascule_statut_a_la_seconde_passe() {
        Utilisateur u = utilisateurMinimal();

        // Passe 1 : civilité + date de naissance seulement (pas l'adresse)
        CompleterMonProfilCivilCommand passe1 = new CompleterMonProfilCivilCommand(
                u.id(), Civilite.MADAME, dateNaissanceValide(), "Bordeaux", "FR", null, null, null
        );
        ResultatCompletionProfil r1 = u.completerProfil(passe1, T);
        assertThat(r1.misAJour().statutProfil()).isEqualTo(StatutProfil.MINIMAL);

        // Passe 2 : adresse (complète les conditions D8)
        CompleterMonProfilCivilCommand passe2 = new CompleterMonProfilCivilCommand(
                r1.misAJour().id(), null, null, null, null, null, adresseValide(), null
        );
        ResultatCompletionProfil r2 = r1.misAJour().completerProfil(passe2, T);
        assertThat(r2.misAJour().statutProfil()).isEqualTo(StatutProfil.COMPLET);
        assertThat(r2.evenements()).hasSize(2); // AdresseDomicileRenseignee + ProfilUtilisateurComplete
    }

    @Test
    void resoumission_identique_est_ignoree_sans_event() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), Civilite.MONSIEUR, null, null, null, null, null, null
        );
        ResultatCompletionProfil r1 = u.completerProfil(cmd, T);

        // Re-soumettre la même civilité : aucun événement
        ResultatCompletionProfil r2 = r1.misAJour().completerProfil(cmd, T);
        assertThat(r2.evenements()).isEmpty();
    }

    @Test
    void profil_complet_ne_remet_pas_ProfilUtilisateurComplete() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), null, dateNaissanceValide(), "Paris", "FR", null, adresseValide(), null
        );
        ResultatCompletionProfil r1 = u.completerProfil(cmd, T);
        assertThat(r1.misAJour().statutProfil()).isEqualTo(StatutProfil.COMPLET);

        // Soumettre à nouveau sans rien de nouveau (identique)
        ResultatCompletionProfil r2 = r1.misAJour().completerProfil(cmd, T);
        assertThat(r2.evenements()).isEmpty();
        assertThat(r2.misAJour().statutProfil()).isEqualTo(StatutProfil.COMPLET);
    }

    @Test
    void telephone_deja_renseigne_a_inscription_nest_pas_renseigne_de_nouveau() {
        // Utilisateur avec téléphone déjà défini à l'inscription
        Utilisateur u = new Utilisateur(
                UtilisateurId.nouveau(),
                new Email("alice@example.com"),
                new HashMotDePasse(HASH),
                "Dupont", "Alice", "+33600000000",
                StatutCompte.ACTIF,
                "cgu-1", T, "conf-1", T, T,
                null, null, null, null, null, null,
                StatutProfil.MINIMAL, null
        );

        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), null, null, null, null, null, null, "+33600000000"
        );

        ResultatCompletionProfil r = u.completerProfil(cmd, T);
        // Pas d'événement TelephoneRenseigne (déjà là)
        assertThat(r.evenements()).isEmpty();
    }

    @Test
    void DonneesNaissanceRenseignees_contient_nationalite_si_fournie() {
        Utilisateur u = utilisateurMinimal();
        CompleterMonProfilCivilCommand cmd = new CompleterMonProfilCivilCommand(
                u.id(), null, dateNaissanceValide(), "Paris", "FR", "BE", null, null
        );
        ResultatCompletionProfil r = u.completerProfil(cmd, T);
        DonneesNaissanceRenseignees evt = r.evenements().stream()
                .filter(e -> e instanceof DonneesNaissanceRenseignees)
                .map(e -> (DonneesNaissanceRenseignees) e)
                .findFirst().orElseThrow();
        assertThat(evt.nationaliteIso()).isEqualTo("BE");
    }

    // =========================================================
    // Cas de refus (I-10)
    // =========================================================

    @Test
    void modification_civilite_levee_exception_I10() {
        Utilisateur u = utilisateurMinimal();
        ResultatCompletionProfil r1 = u.completerProfil(
                new CompleterMonProfilCivilCommand(u.id(), Civilite.MADAME, null, null, null, null, null, null),
                T
        );

        assertThatThrownBy(() ->
                r1.misAJour().completerProfil(
                        new CompleterMonProfilCivilCommand(r1.misAJour().id(), Civilite.MONSIEUR, null, null, null, null, null, null),
                        T
                )
        ).isInstanceOf(ModificationProfilRefuseeException.class)
                .hasMessageContaining("civilite");
    }

    @Test
    void modification_date_naissance_levee_exception_I10() {
        Utilisateur u = utilisateurMinimal();
        ResultatCompletionProfil r1 = u.completerProfil(
                new CompleterMonProfilCivilCommand(u.id(), null, LocalDate.of(1990, 1, 1), "Paris", "FR", null, null, null),
                T
        );

        assertThatThrownBy(() ->
                r1.misAJour().completerProfil(
                        new CompleterMonProfilCivilCommand(r1.misAJour().id(), null, LocalDate.of(1991, 1, 1), null, null, null, null, null),
                        T
                )
        ).isInstanceOf(ModificationProfilRefuseeException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void modification_telephone_levee_exception_I10() {
        Utilisateur u = utilisateurMinimal();
        ResultatCompletionProfil r1 = u.completerProfil(
                new CompleterMonProfilCivilCommand(u.id(), null, null, null, null, null, null, "+33600000001"),
                T
        );

        assertThatThrownBy(() ->
                r1.misAJour().completerProfil(
                        new CompleterMonProfilCivilCommand(r1.misAJour().id(), null, null, null, null, null, null, "+33600000002"),
                        T
                )
        ).isInstanceOf(ModificationProfilRefuseeException.class)
                .hasMessageContaining("telephone");
    }

    // =========================================================
    // Invariants I-2, I-3
    // =========================================================

    @Test
    void date_naissance_mineur_refuse_I2() {
        Utilisateur u = utilisateurMinimal();
        LocalDate moinsDe18Ans = LocalDate.of(2015, 1, 1); // trop récent

        assertThatThrownBy(() ->
                u.completerProfil(
                        new CompleterMonProfilCivilCommand(u.id(), null, moinsDe18Ans, "Paris", "FR", null, null, null),
                        T
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("majeur");
    }

    @Test
    void date_naissance_avant_1900_refuse_I3() {
        Utilisateur u = utilisateurMinimal();
        LocalDate avant1900 = LocalDate.of(1899, 12, 31);

        assertThatThrownBy(() ->
                u.completerProfil(
                        new CompleterMonProfilCivilCommand(u.id(), null, avant1900, "Paris", "FR", null, null, null),
                        T
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================
    // Invariants I-5, I-6 — codes pays invalides
    // =========================================================

    @Test
    void code_pays_naissance_invalide_refuse_I5() {
        Utilisateur u = utilisateurMinimal();

        assertThatThrownBy(() ->
                new Utilisateur(
                        u.id(), u.email(), u.hashMotDePasse(), u.nom(), u.prenom(), null,
                        StatutCompte.ACTIF, "cgu-1", T, "conf-1", T, T,
                        null, null, null, "XX", null, null,
                        StatutProfil.MINIMAL, null
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lieuNaissancePaysIso");
    }

    @Test
    void code_pays_adresse_invalide_refuse_I8() {
        assertThatThrownBy(() ->
                new Adresse("12", "Rue de la Paix", null, "75001", "Paris", "ZZ")
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paysIso");
    }
}

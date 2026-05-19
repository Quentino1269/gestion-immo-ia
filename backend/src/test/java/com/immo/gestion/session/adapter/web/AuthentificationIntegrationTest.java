package com.immo.gestion.session.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.session.adapter.persistence.SessionEntity;
import com.immo.gestion.session.adapter.persistence.SessionJpaRepository;
import com.immo.gestion.session.domain.EtatSession;
import com.immo.gestion.session.domain.MotifFermeture;
import com.immo.gestion.session.domain.RaisonEchecConnexion;
import com.immo.gestion.session.test.CapteurEventsSession;
import com.immo.gestion.utilisateur.adapter.persistence.UtilisateurEntity;
import com.immo.gestion.utilisateur.adapter.persistence.UtilisateurJpaRepository;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurCommand;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@Disabled("Défaillance socket Docker Desktop 29 sur Mac Intel — à réactiver après fix env")
class AuthentificationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtilisateurJpaRepository utilisateurs;
    @Autowired private SessionJpaRepository sessions;
    @Autowired private CreerUtilisateurUseCase creerUtilisateur;
    @Autowired private CapteurEventsSession capteur;

    private MockMvc mockMvc;

    private static final String EMAIL = "alice@example.com";
    private static final String MOT_DE_PASSE = "ChouchouEtLoulou123";
    private static final String MESSAGE_GENERIQUE = "Identifiants invalides.";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        sessions.deleteAll();
        utilisateurs.deleteAll();
        capteur.clear();
    }

    private UtilisateurId inscrireUtilisateur() {
        return creerUtilisateur.creer(new CreerUtilisateurCommand(
                EMAIL, MOT_DE_PASSE, "Dupont", "Alice", null, true, true
        ));
    }

    private static Map<String, Object> loginPayload(String email, String motDePasse) {
        Map<String, Object> m = new HashMap<>();
        m.put("email", email);
        m.put("motDePasse", motDePasse);
        return m;
    }

    private String corpsJson(Map<String, Object> m) throws Exception {
        return objectMapper.writeValueAsString(m);
    }

    // -------------------- Golden path --------------------

    @Test
    void POST_sessions_avec_credentials_valides_retourne_201_avec_token_et_persiste_session() throws Exception {
        UtilisateurId uid = inscrireUtilisateur();

        MvcResult res = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(EMAIL, MOT_DE_PASSE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expireA").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        String tokenClair = body.get("token").asText();
        String sessionId = body.get("sessionId").asText();
        Instant expireA = Instant.parse(body.get("expireA").asText());

        // Token base64url ≥ 128 bits — 32 octets encodés sans padding = 43 caractères
        assertThat(tokenClair).hasSizeGreaterThanOrEqualTo(22); // 128 bits / 6 ≈ 22 caractères
        assertThat(tokenClair).matches("[A-Za-z0-9_-]+"); // base64url

        // Expiration ~ now + 24h
        assertThat(expireA).isAfter(Instant.now());
        assertThat(expireA).isBefore(Instant.now().plusSeconds(25 * 3600));

        // Session persistée en BDD, état ACTIVE, token en clair JAMAIS stocké
        Optional<SessionEntity> persistee = sessions.findById(java.util.UUID.fromString(sessionId));
        assertThat(persistee).isPresent();
        SessionEntity e = persistee.get();
        assertThat(e.getEtat()).isEqualTo(EtatSession.ACTIVE);
        assertThat(e.getUtilisateurId()).isEqualTo(uid.valeur());
        assertThat(e.getTokenHash()).hasSize(64).matches("[0-9a-f]+");
        assertThat(e.getTokenHash()).isNotEqualTo(tokenClair);

        // Event UtilisateurConnecte émis exactement une fois, aucun échec
        assertThat(capteur.connectes()).hasSize(1);
        assertThat(capteur.echecs()).isEmpty();
        assertThat(capteur.connectes().get(0).utilisateurId().valeur()).isEqualTo(uid.valeur());
    }

    @Test
    void POST_sessions_normalise_email_avant_lookup() throws Exception {
        inscrireUtilisateur();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload("  Alice@Example.COM  ", MOT_DE_PASSE))))
                .andExpect(status().isCreated());

        assertThat(capteur.connectes()).hasSize(1);
    }

    @Test
    void POST_sessions_multi_session_autorisee_pour_meme_utilisateur() throws Exception {
        inscrireUtilisateur();

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(EMAIL, MOT_DE_PASSE))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(EMAIL, MOT_DE_PASSE))))
                .andExpect(status().isCreated());

        assertThat(sessions.findAll()).hasSize(2);
        assertThat(capteur.connectes()).hasSize(2);
    }

    // -------------------- Anti-énumération : réponse strictement identique --------------------

    @Test
    void POST_sessions_email_inconnu_retourne_401_avec_message_generique() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload("inconnu@example.com", MOT_DE_PASSE))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MESSAGE_GENERIQUE));

        assertThat(sessions.findAll()).isEmpty();
        assertThat(capteur.connectes()).isEmpty();
        assertThat(capteur.echecs()).hasSize(1);
        assertThat(capteur.echecs().get(0).raison()).isEqualTo(RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
    }

    @Test
    void POST_sessions_mot_de_passe_invalide_retourne_meme_reponse_que_email_inconnu() throws Exception {
        inscrireUtilisateur();

        String corpsEmailInconnu = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload("inconnu@example.com", "MotDePasseSolide!"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String corpsMdpInvalide = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(EMAIL, "MauvaisMdpDe12!"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Corps strictement identique = aucune information d'énumération côté API
        assertThat(corpsEmailInconnu).isEqualTo(corpsMdpInvalide);

        // Côté serveur, deux raisons IDENTIFIANTS_INVALIDES (mais aucun COMPTE_INACTIF)
        assertThat(capteur.echecs()).hasSize(2);
        assertThat(capteur.echecs())
                .allMatch(e -> e.raison() == RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
    }

    @Test
    void POST_sessions_compte_inactif_renvoie_meme_reponse_que_credentials_invalides() throws Exception {
        inscrireUtilisateur();
        // Force le statut SUSPENDU directement en BDD pour simuler un compte inactif
        UtilisateurEntity u = utilisateurs.findAll().get(0);
        utilisateurs.save(new UtilisateurEntity(
                u.getId(), u.getEmail(), u.getHashMotDePasse(), u.getNom(), u.getPrenom(),
                u.getTelephone(), StatutCompte.SUSPENDU, u.getVersionCgu(), u.getCguAccepteesLe(),
                u.getVersionConfidentialite(), u.getConfidentialiteAccepteeLe(), u.getInscritLe()
        ));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(EMAIL, MOT_DE_PASSE))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MESSAGE_GENERIQUE));

        assertThat(sessions.findAll()).isEmpty();
        // Côté serveur en revanche, l'event d'audit porte bien la raison COMPTE_INACTIF
        assertThat(capteur.echecs()).hasSize(1);
        assertThat(capteur.echecs().get(0).raison()).isEqualTo(RaisonEchecConnexion.COMPTE_INACTIF);
    }

    @Test
    void POST_sessions_email_mal_forme_renvoie_meme_reponse_generique() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload("pas-un-email", MOT_DE_PASSE))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MESSAGE_GENERIQUE));

        assertThat(capteur.echecs()).hasSize(1);
        assertThat(capteur.echecs().get(0).raison()).isEqualTo(RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
    }

    @Test
    void POST_sessions_champs_manquants_renvoient_aussi_la_meme_reponse_401() throws Exception {
        Map<String, Object> incomplet = new HashMap<>();
        incomplet.put("email", "");
        incomplet.put("motDePasse", "");

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(incomplet)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MESSAGE_GENERIQUE));
    }

    // -------------------- Filtre Bearer + déconnexion --------------------

    @Test
    void DELETE_sessions_avec_bearer_valide_ferme_session_et_emet_event() throws Exception {
        UtilisateurId uid = inscrireUtilisateur();

        JsonNode login = loginEtRecupererBody();
        String tokenClair = login.get("token").asText();
        String sessionId = login.get("sessionId").asText();

        mockMvc.perform(delete("/api/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenClair))
                .andExpect(status().isNoContent());

        SessionEntity persistee = sessions.findById(java.util.UUID.fromString(sessionId)).orElseThrow();
        assertThat(persistee.getEtat()).isEqualTo(EtatSession.FERMEE);
        assertThat(persistee.getMotifFermeture()).isEqualTo(MotifFermeture.VOLONTAIRE);
        assertThat(persistee.getFermeeLe()).isNotNull();

        assertThat(capteur.deconnectes()).hasSize(1);
        assertThat(capteur.deconnectes().get(0).utilisateurId().valeur()).isEqualTo(uid.valeur());
        assertThat(capteur.deconnectes().get(0).motif()).isEqualTo(MotifFermeture.VOLONTAIRE);
    }

    @Test
    void DELETE_sessions_sans_header_retourne_401() throws Exception {
        inscrireUtilisateur();
        JsonNode login = loginEtRecupererBody();
        String sessionId = login.get("sessionId").asText();

        mockMvc.perform(delete("/api/sessions/" + sessionId))
                .andExpect(status().isUnauthorized());

        assertThat(sessions.findById(java.util.UUID.fromString(sessionId)).orElseThrow().getEtat())
                .isEqualTo(EtatSession.ACTIVE);
    }

    @Test
    void DELETE_sessions_avec_token_inconnu_retourne_401() throws Exception {
        inscrireUtilisateur();
        JsonNode login = loginEtRecupererBody();
        String sessionId = login.get("sessionId").asText();

        mockMvc.perform(delete("/api/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-bidon-inconnu-du-serveur"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void DELETE_sessions_avec_header_mal_forme_retourne_401() throws Exception {
        inscrireUtilisateur();
        JsonNode login = loginEtRecupererBody();
        String sessionId = login.get("sessionId").asText();
        String tokenClair = login.get("token").asText();

        // Pas de préfixe Bearer
        mockMvc.perform(delete("/api/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, tokenClair))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void DELETE_sessions_d_un_autre_utilisateur_est_refuse() throws Exception {
        // Utilisateur A inscrit + login → token A
        inscrireUtilisateur();
        JsonNode loginA = loginEtRecupererBody();
        String tokenA = loginA.get("token").asText();

        // Utilisateur B inscrit + login → on récupère son sessionId
        creerUtilisateur.creer(new CreerUtilisateurCommand(
                "bob@example.com", MOT_DE_PASSE, "Bob", "Bobby", null, true, true
        ));
        JsonNode loginB = loginCustom("bob@example.com", MOT_DE_PASSE);
        String sessionIdB = loginB.get("sessionId").asText();

        // A tente de fermer la session de B avec son propre token
        mockMvc.perform(delete("/api/sessions/" + sessionIdB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isUnauthorized());

        // La session de B est toujours ACTIVE
        assertThat(sessions.findById(java.util.UUID.fromString(sessionIdB)).orElseThrow().getEtat())
                .isEqualTo(EtatSession.ACTIVE);
        assertThat(capteur.deconnectes()).isEmpty();
    }

    @Test
    void le_token_d_une_session_fermee_ne_permet_plus_d_authentifier() throws Exception {
        inscrireUtilisateur();
        JsonNode login = loginEtRecupererBody();
        String token = login.get("token").asText();
        String sessionId = login.get("sessionId").asText();

        // Premier logout : OK
        mockMvc.perform(delete("/api/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // Deuxième tentative avec le même token : la session étant FERMEE,
        // le filter ne pose pas le contexte → controller renvoie 401.
        mockMvc.perform(delete("/api/sessions/" + sessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());

        assertThat(capteur.deconnectes()).hasSize(1);
    }

    // -------------------- Helpers --------------------

    private JsonNode loginEtRecupererBody() throws Exception {
        return loginCustom(EMAIL, MOT_DE_PASSE);
    }

    private JsonNode loginCustom(String email, String motDePasse) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsJson(loginPayload(email, motDePasse))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    /**
     * Sanity check : sur la totalité des scénarios d'échec exécutés ici, le
     * corps de réponse est strictement identique. C'est la garantie observable
     * (par l'attaquant) du contrat anti-énumération D7.
     */
    @SuppressWarnings("unused")
    private void assertCorpsEchecIdentiques(List<String> corps) {
        assertThat(corps).isNotEmpty();
        String reference = corps.get(0);
        for (String c : corps) {
            assertThat(c).isEqualTo(reference);
        }
    }
}

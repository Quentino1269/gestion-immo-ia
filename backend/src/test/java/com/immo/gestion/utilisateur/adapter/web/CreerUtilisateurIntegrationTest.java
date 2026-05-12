package com.immo.gestion.utilisateur.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immo.gestion.utilisateur.adapter.persistence.UtilisateurEntity;
import com.immo.gestion.utilisateur.adapter.persistence.UtilisateurJpaRepository;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import com.immo.gestion.utilisateur.test.CapteurEventsUtilisateurInscrit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class CreerUtilisateurIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurJpaRepository jpaRepository;

    @Autowired
    private CapteurEventsUtilisateurInscrit capteur;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jpaRepository.deleteAll();
        capteur.clear();
    }

    private static Map<String, Object> payloadValide() {
        Map<String, Object> m = new HashMap<>();
        m.put("email", "Alice@Example.com");
        m.put("motDePasse", "MotDePasseSolide!");
        m.put("nom", "Dupont");
        m.put("prenom", "Alice");
        m.put("telephone", "+33612345678");
        m.put("accepteCgu", true);
        m.put("accepteConfidentialite", true);
        return m;
    }

    @Test
    void post_inscription_valide_retourne_201_persiste_et_emet_event() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValide())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.utilisateurId").isNotEmpty());

        List<UtilisateurEntity> stockes = jpaRepository.findAll();
        assertThat(stockes).hasSize(1);
        UtilisateurEntity u = stockes.get(0);
        assertThat(u.getEmail()).isEqualTo("alice@example.com");
        assertThat(u.getNom()).isEqualTo("Dupont");
        assertThat(u.getPrenom()).isEqualTo("Alice");
        assertThat(u.getTelephone()).isEqualTo("+33612345678");
        assertThat(u.getStatut()).isEqualTo(StatutCompte.ACTIF);
        assertThat(u.getHashMotDePasse()).startsWith("$argon2id$");

        assertThat(capteur.events()).hasSize(1);
        UtilisateurInscrit event = capteur.events().get(0);
        assertThat(event.email().valeur()).isEqualTo("alice@example.com");
        assertThat(event.statut()).isEqualTo(StatutCompte.ACTIF);
    }

    @Test
    void post_inscription_email_duplicate_retourne_400_avec_message_generique() throws Exception {
        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValide())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValide())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cette adresse ne peut pas être utilisée."));

        assertThat(jpaRepository.findAll()).hasSize(1);
    }

    @Test
    void post_inscription_sans_cgu_retourne_400() throws Exception {
        Map<String, Object> sansCgu = new HashMap<>(payloadValide());
        sansCgu.put("accepteCgu", false);

        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sansCgu)))
                .andExpect(status().isBadRequest());

        assertThat(jpaRepository.findAll()).isEmpty();
        assertThat(capteur.events()).isEmpty();
    }

    @Test
    void post_inscription_motdepasse_court_retourne_400() throws Exception {
        Map<String, Object> court = new HashMap<>(payloadValide());
        court.put("motDePasse", "court");

        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(court)))
                .andExpect(status().isBadRequest());

        assertThat(jpaRepository.findAll()).isEmpty();
    }

    @Test
    void post_inscription_email_malforme_retourne_400() throws Exception {
        Map<String, Object> mauvais = new HashMap<>(payloadValide());
        mauvais.put("email", "pas-un-email");

        mockMvc.perform(post("/api/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mauvais)))
                .andExpect(status().isBadRequest());
    }
}

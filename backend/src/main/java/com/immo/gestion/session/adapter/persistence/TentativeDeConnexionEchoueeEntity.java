package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.RaisonEchecConnexion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Journal d'audit des tentatives de connexion échouées (D7, D9). Fait métier sans aggregate :
 * n'entre pas dans {@code event_store}, persisté directement ici. Cf. MISSION.md §5.
 */
@Entity
@Table(name = "tentatives_connexion_echouees")
public class TentativeDeConnexionEchoueeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_soumis", nullable = false, length = 254)
    private String emailSoumis;

    @Enumerated(EnumType.STRING)
    @Column(name = "raison", nullable = false, length = 32)
    private RaisonEchecConnexion raison;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_source", length = 64)
    private String ipSource;

    @Column(name = "survenu_le", nullable = false)
    private Instant survenuLe;

    protected TentativeDeConnexionEchoueeEntity() {
        // JPA
    }

    public TentativeDeConnexionEchoueeEntity(String emailSoumis, RaisonEchecConnexion raison,
                                              String userAgent, String ipSource, Instant survenuLe) {
        this.emailSoumis = emailSoumis;
        this.raison = raison;
        this.userAgent = userAgent;
        this.ipSource = ipSource;
        this.survenuLe = survenuLe;
    }

    public Long getId() { return id; }
    public String getEmailSoumis() { return emailSoumis; }
    public RaisonEchecConnexion getRaison() { return raison; }
    public String getUserAgent() { return userAgent; }
    public String getIpSource() { return ipSource; }
    public Instant getSurvenuLe() { return survenuLe; }
}

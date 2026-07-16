package com.immo.gestion.session.adapter.persistence;

import com.immo.gestion.session.domain.EtatSession;
import com.immo.gestion.session.domain.MotifFermeture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sessions",
        indexes = @Index(name = "idx_sessions_token_hash", columnList = "token_hash", unique = true)
)
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "ouverte_le", nullable = false)
    private Instant ouverteLe;

    @Column(name = "expire_a", nullable = false)
    private Instant expireA;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 16)
    private EtatSession etat;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_fermeture", length = 16)
    private MotifFermeture motifFermeture;

    @Column(name = "fermee_le")
    private Instant fermeeLe;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_source", length = 64)
    private String ipSource;

    protected SessionEntity() {
        // JPA
    }

    public SessionEntity(
            UUID id,
            UUID utilisateurId,
            String tokenHash,
            Instant ouverteLe,
            Instant expireA,
            EtatSession etat,
            MotifFermeture motifFermeture,
            Instant fermeeLe,
            String userAgent,
            String ipSource
    ) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.tokenHash = tokenHash;
        this.ouverteLe = ouverteLe;
        this.expireA = expireA;
        this.etat = etat;
        this.motifFermeture = motifFermeture;
        this.fermeeLe = fermeeLe;
        this.userAgent = userAgent;
        this.ipSource = ipSource;
    }

    public UUID getId() { return id; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getOuverteLe() { return ouverteLe; }
    public Instant getExpireA() { return expireA; }
    public EtatSession getEtat() { return etat; }
    public MotifFermeture getMotifFermeture() { return motifFermeture; }
    public Instant getFermeeLe() { return fermeeLe; }
    public String getUserAgent() { return userAgent; }
    public String getIpSource() { return ipSource; }

    // Projection (read model) honnêtement mutable : mise à jour partielle par SessionProjectionListener
    // à la fermeture d'une session, sans reconstruire toute la ligne. Cf. MISSION.md §5.
    public void setEtat(EtatSession etat) { this.etat = etat; }
    public void setMotifFermeture(MotifFermeture motifFermeture) { this.motifFermeture = motifFermeture; }
    public void setFermeeLe(Instant fermeeLe) { this.fermeeLe = fermeeLe; }
}

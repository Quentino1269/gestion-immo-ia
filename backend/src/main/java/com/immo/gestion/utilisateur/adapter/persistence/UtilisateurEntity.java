package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.utilisateur.domain.StatutCompte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs")
public class UtilisateurEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "hash_mot_de_passe", nullable = false, length = 200)
    private String hashMotDePasse;

    @Column(name = "nom", nullable = false, length = 80)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 80)
    private String prenom;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 16)
    private StatutCompte statut;

    @Column(name = "version_cgu", nullable = false, length = 80)
    private String versionCgu;

    @Column(name = "cgu_acceptees_le", nullable = false)
    private Instant cguAccepteesLe;

    @Column(name = "version_confidentialite", nullable = false, length = 80)
    private String versionConfidentialite;

    @Column(name = "confidentialite_acceptee_le", nullable = false)
    private Instant confidentialiteAccepteeLe;

    @Column(name = "inscrit_le", nullable = false)
    private Instant inscritLe;

    protected UtilisateurEntity() {
        // JPA
    }

    public UtilisateurEntity(
            UUID id,
            String email,
            String hashMotDePasse,
            String nom,
            String prenom,
            String telephone,
            StatutCompte statut,
            String versionCgu,
            Instant cguAccepteesLe,
            String versionConfidentialite,
            Instant confidentialiteAccepteeLe,
            Instant inscritLe
    ) {
        this.id = id;
        this.email = email;
        this.hashMotDePasse = hashMotDePasse;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.statut = statut;
        this.versionCgu = versionCgu;
        this.cguAccepteesLe = cguAccepteesLe;
        this.versionConfidentialite = versionConfidentialite;
        this.confidentialiteAccepteeLe = confidentialiteAccepteeLe;
        this.inscritLe = inscritLe;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getHashMotDePasse() { return hashMotDePasse; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getTelephone() { return telephone; }
    public StatutCompte getStatut() { return statut; }
    public String getVersionCgu() { return versionCgu; }
    public Instant getCguAccepteesLe() { return cguAccepteesLe; }
    public String getVersionConfidentialite() { return versionConfidentialite; }
    public Instant getConfidentialiteAccepteeLe() { return confidentialiteAccepteeLe; }
    public Instant getInscritLe() { return inscritLe; }
}

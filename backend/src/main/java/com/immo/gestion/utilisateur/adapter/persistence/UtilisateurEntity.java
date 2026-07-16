package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.utilisateur.domain.Civilite;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.StatutProfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
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

    // --- Profil civil (slice enrichissement) ---

    @Enumerated(EnumType.STRING)
    @Column(name = "civilite", length = 16)
    private Civilite civilite;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance_ville", length = 80)
    private String lieuNaissanceVille;

    @Column(name = "lieu_naissance_pays_iso", length = 2)
    private String lieuNaissancePaysIso;

    @Column(name = "nationalite_iso", length = 2)
    private String nationaliteIso;

    @Column(name = "adresse_numero", length = 20)
    private String adresseNumero;

    @Column(name = "adresse_voie", length = 150)
    private String adresseVoie;

    @Column(name = "adresse_complement", length = 100)
    private String adresseComplement;

    @Column(name = "adresse_code_postal", length = 10)
    private String adresseCodePostal;

    @Column(name = "adresse_commune", length = 100)
    private String adresseCommune;

    @Column(name = "adresse_pays_iso", length = 2)
    private String adressePaysIso;

    // Nullable en base pour la migration ddl-auto=update ; toujours MINIMAL si null
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_profil", length = 16)
    private StatutProfil statutProfil;

    @Column(name = "profil_complete_le")
    private Instant profilCompleteLe;

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
            Instant inscritLe,
            Civilite civilite,
            LocalDate dateNaissance,
            String lieuNaissanceVille,
            String lieuNaissancePaysIso,
            String nationaliteIso,
            String adresseNumero,
            String adresseVoie,
            String adresseComplement,
            String adresseCodePostal,
            String adresseCommune,
            String adressePaysIso,
            StatutProfil statutProfil,
            Instant profilCompleteLe
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
        this.civilite = civilite;
        this.dateNaissance = dateNaissance;
        this.lieuNaissanceVille = lieuNaissanceVille;
        this.lieuNaissancePaysIso = lieuNaissancePaysIso;
        this.nationaliteIso = nationaliteIso;
        this.adresseNumero = adresseNumero;
        this.adresseVoie = adresseVoie;
        this.adresseComplement = adresseComplement;
        this.adresseCodePostal = adresseCodePostal;
        this.adresseCommune = adresseCommune;
        this.adressePaysIso = adressePaysIso;
        this.statutProfil = statutProfil;
        this.profilCompleteLe = profilCompleteLe;
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
    public Civilite getCivilite() { return civilite; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public String getLieuNaissanceVille() { return lieuNaissanceVille; }
    public String getLieuNaissancePaysIso() { return lieuNaissancePaysIso; }
    public String getNationaliteIso() { return nationaliteIso; }
    public String getAdresseNumero() { return adresseNumero; }
    public String getAdresseVoie() { return adresseVoie; }
    public String getAdresseComplement() { return adresseComplement; }
    public String getAdresseCodePostal() { return adresseCodePostal; }
    public String getAdresseCommune() { return adresseCommune; }
    public String getAdressePaysIso() { return adressePaysIso; }
    public StatutProfil getStatutProfil() { return statutProfil; }
    public Instant getProfilCompleteLe() { return profilCompleteLe; }

    // Projection (read model) honnêtement mutable : mises à jour partielles par
    // UtilisateurProjectionListener au fil des événements d'enrichissement du profil.
    // Cf. MISSION.md §5.
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setCivilite(Civilite civilite) { this.civilite = civilite; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public void setLieuNaissanceVille(String lieuNaissanceVille) { this.lieuNaissanceVille = lieuNaissanceVille; }
    public void setLieuNaissancePaysIso(String lieuNaissancePaysIso) { this.lieuNaissancePaysIso = lieuNaissancePaysIso; }
    public void setNationaliteIso(String nationaliteIso) { this.nationaliteIso = nationaliteIso; }
    public void setAdresseNumero(String adresseNumero) { this.adresseNumero = adresseNumero; }
    public void setAdresseVoie(String adresseVoie) { this.adresseVoie = adresseVoie; }
    public void setAdresseComplement(String adresseComplement) { this.adresseComplement = adresseComplement; }
    public void setAdresseCodePostal(String adresseCodePostal) { this.adresseCodePostal = adresseCodePostal; }
    public void setAdresseCommune(String adresseCommune) { this.adresseCommune = adresseCommune; }
    public void setAdressePaysIso(String adressePaysIso) { this.adressePaysIso = adressePaysIso; }
    public void setStatutProfil(StatutProfil statutProfil) { this.statutProfil = statutProfil; }
    public void setProfilCompleteLe(Instant profilCompleteLe) { this.profilCompleteLe = profilCompleteLe; }
}

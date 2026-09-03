package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.TypeBien;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "biens")
public class BienEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proprietaire_initial_id", nullable = false, updatable = false)
    private UUID proprietaireInitialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_bien", nullable = false, length = 24)
    private TypeBien typeBien;

    @Column(name = "bien_parent_id")
    private UUID bienParentId;

    @Column(name = "libelle_chambre", length = 50)
    private String libelleChambre;

    @Column(name = "nb_pieces_principales", nullable = false)
    private int nbPiecesPrincipales;

    @Column(name = "surface_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal surfaceM2;

    @Column(name = "meuble", nullable = false)
    private boolean meuble;

    @Column(name = "loyer_hors_charges_centimes", nullable = false)
    private long loyerHorsChargesEnCentimes;

    @Column(name = "charges_centimes", nullable = false)
    private long chargesEnCentimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalite_charges", nullable = false, length = 16)
    private ModaliteCharges modaliteCharges;

    @Column(name = "adresse_numero", nullable = false, length = 20)
    private String adresseNumero;

    @Column(name = "adresse_voie", nullable = false, length = 150)
    private String adresseVoie;

    @Column(name = "adresse_complement", length = 100)
    private String adresseComplement;

    @Column(name = "adresse_code_postal", nullable = false, length = 10)
    private String adresseCodePostal;

    @Column(name = "adresse_commune", nullable = false, length = 100)
    private String adresseCommune;

    @Column(name = "adresse_pays_iso", nullable = false, length = 2)
    private String adressePaysIso;

    @Column(name = "disponible_a_partir_du", nullable = false)
    private LocalDate disponibleAPartirDu;

    @Column(name = "ajoute_le", nullable = false, updatable = false)
    private Instant ajouteLe;

    protected BienEntity() {
        // JPA
    }

    public BienEntity(
            UUID id,
            UUID proprietaireInitialId,
            TypeBien typeBien,
            UUID bienParentId,
            String libelleChambre,
            int nbPiecesPrincipales,
            BigDecimal surfaceM2,
            boolean meuble,
            long loyerHorsChargesEnCentimes,
            long chargesEnCentimes,
            ModaliteCharges modaliteCharges,
            String adresseNumero,
            String adresseVoie,
            String adresseComplement,
            String adresseCodePostal,
            String adresseCommune,
            String adressePaysIso,
            LocalDate disponibleAPartirDu,
            Instant ajouteLe
    ) {
        this.id = id;
        this.proprietaireInitialId = proprietaireInitialId;
        this.typeBien = typeBien;
        this.bienParentId = bienParentId;
        this.libelleChambre = libelleChambre;
        this.nbPiecesPrincipales = nbPiecesPrincipales;
        this.surfaceM2 = surfaceM2;
        this.meuble = meuble;
        this.loyerHorsChargesEnCentimes = loyerHorsChargesEnCentimes;
        this.chargesEnCentimes = chargesEnCentimes;
        this.modaliteCharges = modaliteCharges;
        this.adresseNumero = adresseNumero;
        this.adresseVoie = adresseVoie;
        this.adresseComplement = adresseComplement;
        this.adresseCodePostal = adresseCodePostal;
        this.adresseCommune = adresseCommune;
        this.adressePaysIso = adressePaysIso;
        this.disponibleAPartirDu = disponibleAPartirDu;
        this.ajouteLe = ajouteLe;
    }

    public UUID getId() { return id; }
    public UUID getProprietaireInitialId() { return proprietaireInitialId; }
    public TypeBien getTypeBien() { return typeBien; }
    public UUID getBienParentId() { return bienParentId; }
    public String getLibelleChambre() { return libelleChambre; }
    public int getNbPiecesPrincipales() { return nbPiecesPrincipales; }
    public BigDecimal getSurfaceM2() { return surfaceM2; }
    public boolean isMeuble() { return meuble; }
    public long getLoyerHorsChargesEnCentimes() { return loyerHorsChargesEnCentimes; }
    public long getChargesEnCentimes() { return chargesEnCentimes; }
    public ModaliteCharges getModaliteCharges() { return modaliteCharges; }
    public String getAdresseNumero() { return adresseNumero; }
    public String getAdresseVoie() { return adresseVoie; }
    public String getAdresseComplement() { return adresseComplement; }
    public String getAdresseCodePostal() { return adresseCodePostal; }
    public String getAdresseCommune() { return adresseCommune; }
    public String getAdressePaysIso() { return adressePaysIso; }
    public LocalDate getDisponibleAPartirDu() { return disponibleAPartirDu; }
    public Instant getAjouteLe() { return ajouteLe; }

    public void setLoyerHorsChargesEnCentimes(long loyerHorsChargesEnCentimes) {
        this.loyerHorsChargesEnCentimes = loyerHorsChargesEnCentimes;
    }
    public void setChargesEnCentimes(long chargesEnCentimes) { this.chargesEnCentimes = chargesEnCentimes; }
    public void setMeuble(boolean meuble) { this.meuble = meuble; }
    public void setModaliteCharges(ModaliteCharges modaliteCharges) { this.modaliteCharges = modaliteCharges; }
    public void setDisponibleAPartirDu(LocalDate disponibleAPartirDu) { this.disponibleAPartirDu = disponibleAPartirDu; }
    public void setLibelleChambre(String libelleChambre) { this.libelleChambre = libelleChambre; }
    public void setNbPiecesPrincipales(int nbPiecesPrincipales) { this.nbPiecesPrincipales = nbPiecesPrincipales; }
}

package com.immo.gestion.rentabilite.adapter.persistence;

import com.immo.gestion.rentabilite.domain.RegimeFiscal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulations_rentabilite")
public class SimulationRentabiliteEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "bien_id", nullable = false, updatable = false)
    private UUID bienId;

    @Column(name = "utilisateur_id", nullable = false, updatable = false)
    private UUID utilisateurId;

    @Column(name = "nom_scenario", nullable = false, length = 100)
    private String nomScenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_fiscal", nullable = false, length = 20)
    private RegimeFiscal regimeFiscal;

    @Column(name = "tmi_foyer_pourcent", nullable = false)
    private int tmiFoyerPourcent;

    @Column(name = "horizon_annees", nullable = false)
    private int horizonAnnees;

    @Column(name = "prix_achat_centimes", nullable = false)
    private long prixAchatEnCentimes;

    @Column(name = "frais_notaire_centimes", nullable = false)
    private long fraisNotaireEnCentimes;

    @Column(name = "frais_agence_centimes", nullable = false)
    private long fraisAgenceEnCentimes;

    @Column(name = "travaux_acquisition_centimes", nullable = false)
    private long travauxAlAcquisitionEnCentimes;

    @Column(name = "frais_dossier_bancaire_centimes", nullable = false)
    private long fraisDossierBancaireEnCentimes;

    @Column(name = "montant_emprunte_centimes", nullable = false)
    private long montantEmprunteEnCentimes;

    @Column(name = "taux_annuel_pourcent", nullable = false, precision = 10, scale = 4)
    private BigDecimal tauxAnnuelPourcent;

    @Column(name = "duree_annees", nullable = false)
    private int dureeAnnees;

    @Column(name = "taux_assurance_emprunteur_pourcent", nullable = false, precision = 10, scale = 4)
    private BigDecimal tauxAssuranceEmprunteurPourcent;

    @Column(name = "quote_part_terrain_pourcent", nullable = false, precision = 6, scale = 2)
    private BigDecimal quotePartTerrainPourcent;

    @Column(name = "quote_part_mobilier_pourcent", nullable = false, precision = 6, scale = 2)
    private BigDecimal quotePartMobilierPourcent;

    @Column(name = "duree_amortissement_bati_annees", nullable = false)
    private int dureeAmortissementBatiAnnees;

    @Column(name = "duree_amortissement_mobilier_annees", nullable = false)
    private int dureeAmortissementMobilierAnnees;

    @Column(name = "taxe_fonciere_centimes", nullable = false)
    private long taxeFonciereEnCentimes;

    @Column(name = "assurance_pno_centimes", nullable = false)
    private long assurancePnoEnCentimes;

    @Column(name = "assurance_loyers_impayes_centimes", nullable = false)
    private long assuranceLoyersImpayesEnCentimes;

    @Column(name = "frais_gestion_locative_pourcent_loyer", nullable = false, precision = 6, scale = 2)
    private BigDecimal fraisGestionLocativePourcentLoyer;

    @Column(name = "provision_travaux_annuelle_centimes", nullable = false)
    private long provisionTravauxAnnuelleEnCentimes;

    @Column(name = "frais_comptabilite_annuel_centimes", nullable = false)
    private long fraisComptabiliteAnnuelEnCentimes;

    @Column(name = "charges_copro_non_recuperables_centimes", nullable = false)
    private long chargesCoproprieteNonRecuperablesEnCentimes;

    @Column(name = "taux_vacance_locative_pourcent", nullable = false, precision = 6, scale = 2)
    private BigDecimal tauxVacanceLocativePourcent;

    @Column(name = "taux_indexation_loyer_pourcent", nullable = false, precision = 6, scale = 2)
    private BigDecimal tauxIndexationLoyerPourcent;

    @Column(name = "taux_indexation_charges_pourcent", nullable = false, precision = 6, scale = 2)
    private BigDecimal tauxIndexationChargesPourcent;

    @Column(name = "cout_total_acquisition_centimes", nullable = false)
    private long coutTotalAcquisitionEnCentimes;

    @Column(name = "apport_personnel_centimes", nullable = false)
    private long apportPersonnelEnCentimes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revenus_locatifs_simules", nullable = false)
    private String revenusLocatifsSimulesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "projection_annuelle", nullable = false)
    private String projectionAnnuelleJson;

    // Pas d'updatable = false : ce champ représente la date de calcul de la version COURANTE
    // (recalculée à chaque modification, cf. SimulationRentabiliteModifiee), pas la date de création.
    @Column(name = "simule_le", nullable = false)
    private Instant simuleLe;

    protected SimulationRentabiliteEntity() {
        // JPA
    }

    public SimulationRentabiliteEntity(
            UUID id, UUID bienId, UUID utilisateurId, String nomScenario, RegimeFiscal regimeFiscal,
            int tmiFoyerPourcent, int horizonAnnees,
            long prixAchatEnCentimes, long fraisNotaireEnCentimes, long fraisAgenceEnCentimes,
            long travauxAlAcquisitionEnCentimes, long fraisDossierBancaireEnCentimes,
            long montantEmprunteEnCentimes, BigDecimal tauxAnnuelPourcent, int dureeAnnees,
            BigDecimal tauxAssuranceEmprunteurPourcent,
            BigDecimal quotePartTerrainPourcent, BigDecimal quotePartMobilierPourcent,
            int dureeAmortissementBatiAnnees, int dureeAmortissementMobilierAnnees,
            long taxeFonciereEnCentimes, long assurancePnoEnCentimes, long assuranceLoyersImpayesEnCentimes,
            BigDecimal fraisGestionLocativePourcentLoyer, long provisionTravauxAnnuelleEnCentimes,
            long fraisComptabiliteAnnuelEnCentimes, long chargesCoproprieteNonRecuperablesEnCentimes,
            BigDecimal tauxVacanceLocativePourcent, BigDecimal tauxIndexationLoyerPourcent,
            BigDecimal tauxIndexationChargesPourcent,
            long coutTotalAcquisitionEnCentimes, long apportPersonnelEnCentimes,
            String revenusLocatifsSimulesJson, String projectionAnnuelleJson,
            Instant simuleLe
    ) {
        this.id = id;
        this.bienId = bienId;
        this.utilisateurId = utilisateurId;
        this.nomScenario = nomScenario;
        this.regimeFiscal = regimeFiscal;
        this.tmiFoyerPourcent = tmiFoyerPourcent;
        this.horizonAnnees = horizonAnnees;
        this.prixAchatEnCentimes = prixAchatEnCentimes;
        this.fraisNotaireEnCentimes = fraisNotaireEnCentimes;
        this.fraisAgenceEnCentimes = fraisAgenceEnCentimes;
        this.travauxAlAcquisitionEnCentimes = travauxAlAcquisitionEnCentimes;
        this.fraisDossierBancaireEnCentimes = fraisDossierBancaireEnCentimes;
        this.montantEmprunteEnCentimes = montantEmprunteEnCentimes;
        this.tauxAnnuelPourcent = tauxAnnuelPourcent;
        this.dureeAnnees = dureeAnnees;
        this.tauxAssuranceEmprunteurPourcent = tauxAssuranceEmprunteurPourcent;
        this.quotePartTerrainPourcent = quotePartTerrainPourcent;
        this.quotePartMobilierPourcent = quotePartMobilierPourcent;
        this.dureeAmortissementBatiAnnees = dureeAmortissementBatiAnnees;
        this.dureeAmortissementMobilierAnnees = dureeAmortissementMobilierAnnees;
        this.taxeFonciereEnCentimes = taxeFonciereEnCentimes;
        this.assurancePnoEnCentimes = assurancePnoEnCentimes;
        this.assuranceLoyersImpayesEnCentimes = assuranceLoyersImpayesEnCentimes;
        this.fraisGestionLocativePourcentLoyer = fraisGestionLocativePourcentLoyer;
        this.provisionTravauxAnnuelleEnCentimes = provisionTravauxAnnuelleEnCentimes;
        this.fraisComptabiliteAnnuelEnCentimes = fraisComptabiliteAnnuelEnCentimes;
        this.chargesCoproprieteNonRecuperablesEnCentimes = chargesCoproprieteNonRecuperablesEnCentimes;
        this.tauxVacanceLocativePourcent = tauxVacanceLocativePourcent;
        this.tauxIndexationLoyerPourcent = tauxIndexationLoyerPourcent;
        this.tauxIndexationChargesPourcent = tauxIndexationChargesPourcent;
        this.coutTotalAcquisitionEnCentimes = coutTotalAcquisitionEnCentimes;
        this.apportPersonnelEnCentimes = apportPersonnelEnCentimes;
        this.revenusLocatifsSimulesJson = revenusLocatifsSimulesJson;
        this.projectionAnnuelleJson = projectionAnnuelleJson;
        this.simuleLe = simuleLe;
    }

    public UUID getId() { return id; }
    public UUID getBienId() { return bienId; }
    public UUID getUtilisateurId() { return utilisateurId; }
    public String getNomScenario() { return nomScenario; }
    public RegimeFiscal getRegimeFiscal() { return regimeFiscal; }
    public int getTmiFoyerPourcent() { return tmiFoyerPourcent; }
    public int getHorizonAnnees() { return horizonAnnees; }
    public long getPrixAchatEnCentimes() { return prixAchatEnCentimes; }
    public long getFraisNotaireEnCentimes() { return fraisNotaireEnCentimes; }
    public long getFraisAgenceEnCentimes() { return fraisAgenceEnCentimes; }
    public long getTravauxAlAcquisitionEnCentimes() { return travauxAlAcquisitionEnCentimes; }
    public long getFraisDossierBancaireEnCentimes() { return fraisDossierBancaireEnCentimes; }
    public long getMontantEmprunteEnCentimes() { return montantEmprunteEnCentimes; }
    public BigDecimal getTauxAnnuelPourcent() { return tauxAnnuelPourcent; }
    public int getDureeAnnees() { return dureeAnnees; }
    public BigDecimal getTauxAssuranceEmprunteurPourcent() { return tauxAssuranceEmprunteurPourcent; }
    public BigDecimal getQuotePartTerrainPourcent() { return quotePartTerrainPourcent; }
    public BigDecimal getQuotePartMobilierPourcent() { return quotePartMobilierPourcent; }
    public int getDureeAmortissementBatiAnnees() { return dureeAmortissementBatiAnnees; }
    public int getDureeAmortissementMobilierAnnees() { return dureeAmortissementMobilierAnnees; }
    public long getTaxeFonciereEnCentimes() { return taxeFonciereEnCentimes; }
    public long getAssurancePnoEnCentimes() { return assurancePnoEnCentimes; }
    public long getAssuranceLoyersImpayesEnCentimes() { return assuranceLoyersImpayesEnCentimes; }
    public BigDecimal getFraisGestionLocativePourcentLoyer() { return fraisGestionLocativePourcentLoyer; }
    public long getProvisionTravauxAnnuelleEnCentimes() { return provisionTravauxAnnuelleEnCentimes; }
    public long getFraisComptabiliteAnnuelEnCentimes() { return fraisComptabiliteAnnuelEnCentimes; }
    public long getChargesCoproprieteNonRecuperablesEnCentimes() { return chargesCoproprieteNonRecuperablesEnCentimes; }
    public BigDecimal getTauxVacanceLocativePourcent() { return tauxVacanceLocativePourcent; }
    public BigDecimal getTauxIndexationLoyerPourcent() { return tauxIndexationLoyerPourcent; }
    public BigDecimal getTauxIndexationChargesPourcent() { return tauxIndexationChargesPourcent; }
    public long getCoutTotalAcquisitionEnCentimes() { return coutTotalAcquisitionEnCentimes; }
    public long getApportPersonnelEnCentimes() { return apportPersonnelEnCentimes; }
    public String getRevenusLocatifsSimulesJson() { return revenusLocatifsSimulesJson; }
    public String getProjectionAnnuelleJson() { return projectionAnnuelleJson; }
    public Instant getSimuleLe() { return simuleLe; }
}

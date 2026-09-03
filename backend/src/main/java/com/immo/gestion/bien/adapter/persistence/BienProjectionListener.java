package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.bien.domain.ChargesRevisees;
import com.immo.gestion.bien.domain.DisponibiliteRevisee;
import com.immo.gestion.bien.domain.LibelleChambreRenomme;
import com.immo.gestion.bien.domain.LogementDevenuNu;
import com.immo.gestion.bien.domain.LoyerRevise;
import com.immo.gestion.bien.domain.MeubleEntreDansLeLogement;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.NombrePiecesRevise;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.shared.Adresse;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Maintient la projection {@code biens} à jour à partir des événements de l'event store.
 * {@code @EventListener} volontairement synchrone (pas {@code @TransactionalEventListener}) pour
 * s'exécuter dans la même transaction que l'append. Cf. MISSION.md §5. Toujours l'état COURANT
 * (dernière version) ; l'historique complet reste dans l'event store, rejoué à la demande par
 * {@link BienRepositoryAdapter}.
 */
@Component
public class BienProjectionListener {

    private final BienJpaRepository jpa;

    public BienProjectionListener(BienJpaRepository jpa) {
        this.jpa = jpa;
    }

    @EventListener
    public void surBienAjoute(BienAjouteAuPortefeuille evenement) {
        Adresse adresse = evenement.adresse();
        jpa.save(construire(
                evenement.bienId().valeur(), evenement.proprietaireInitialId().valeur(), evenement.typeBien(),
                evenement.bienParentId() != null ? evenement.bienParentId().valeur() : null,
                evenement.libelleChambre(), evenement.nbPiecesPrincipales(), evenement.surfaceM2(), evenement.meuble(),
                evenement.loyerHorsChargesEnCentimes(), evenement.chargesEnCentimes(), evenement.modaliteCharges(),
                adresse.numero(), adresse.voie(), adresse.complement(), adresse.codePostal(), adresse.commune(),
                adresse.paysIso(), evenement.disponibleAPartirDu(), evenement.survenuLe()
        ));
    }

    @EventListener
    public void surLoyerRevise(LoyerRevise evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                e.getNbPiecesPrincipales(), e.getSurfaceM2(), e.isMeuble(), evenement.loyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(), e.getModaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surChargesRevisees(ChargesRevisees evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                e.getNbPiecesPrincipales(), e.getSurfaceM2(), e.isMeuble(), e.getLoyerHorsChargesEnCentimes(),
                evenement.chargesEnCentimes(), e.getModaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surMeubleEntreDansLeLogement(MeubleEntreDansLeLogement evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                e.getNbPiecesPrincipales(), e.getSurfaceM2(), true, e.getLoyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(), evenement.modaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surLogementDevenuNu(LogementDevenuNu evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                e.getNbPiecesPrincipales(), e.getSurfaceM2(), false, e.getLoyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(), evenement.modaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surDisponibiliteRevisee(DisponibiliteRevisee evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                e.getNbPiecesPrincipales(), e.getSurfaceM2(), e.isMeuble(), e.getLoyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(), e.getModaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                evenement.disponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surLibelleChambreRenomme(LibelleChambreRenomme evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(),
                evenement.libelleChambre(), e.getNbPiecesPrincipales(), e.getSurfaceM2(), e.isMeuble(),
                e.getLoyerHorsChargesEnCentimes(), e.getChargesEnCentimes(), e.getModaliteCharges(),
                e.getAdresseNumero(), e.getAdresseVoie(), e.getAdresseComplement(), e.getAdresseCodePostal(),
                e.getAdresseCommune(), e.getAdressePaysIso(), e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    @EventListener
    public void surNombrePiecesRevise(NombrePiecesRevise evenement) {
        BienEntity e = existante(evenement.bienId().valeur());
        jpa.save(construire(
                e.getId(), e.getProprietaireInitialId(), e.getTypeBien(), e.getBienParentId(), e.getLibelleChambre(),
                evenement.nbPiecesPrincipales(), e.getSurfaceM2(), e.isMeuble(), e.getLoyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(), e.getModaliteCharges(), e.getAdresseNumero(), e.getAdresseVoie(),
                e.getAdresseComplement(), e.getAdresseCodePostal(), e.getAdresseCommune(), e.getAdressePaysIso(),
                e.getDisponibleAPartirDu(), e.getAjouteLe()
        ));
    }

    private BienEntity existante(UUID bienId) {
        return jpa.findById(bienId).orElseThrow(() -> new IllegalStateException(
                "Projection biens absente pour " + bienId
                        + " — un événement de modification ne peut pas être le premier du flux"));
    }

    private BienEntity construire(
            UUID id, UUID proprietaireInitialId, TypeBien typeBien, UUID bienParentId, String libelleChambre,
            int nbPiecesPrincipales, BigDecimal surfaceM2, boolean meuble, long loyerHorsChargesEnCentimes,
            long chargesEnCentimes, ModaliteCharges modaliteCharges, String adresseNumero, String adresseVoie,
            String adresseComplement, String adresseCodePostal, String adresseCommune, String adressePaysIso,
            LocalDate disponibleAPartirDu, Instant ajouteLe
    ) {
        return new BienEntity(
                id, proprietaireInitialId, typeBien, bienParentId, libelleChambre, nbPiecesPrincipales, surfaceM2,
                meuble, loyerHorsChargesEnCentimes, chargesEnCentimes, modaliteCharges, adresseNumero, adresseVoie,
                adresseComplement, adresseCodePostal, adresseCommune, adressePaysIso, disponibleAPartirDu, ajouteLe
        );
    }
}

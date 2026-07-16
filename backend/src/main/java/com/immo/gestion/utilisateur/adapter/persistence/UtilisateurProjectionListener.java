package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.AdresseDomicileRenseignee;
import com.immo.gestion.utilisateur.domain.CiviliteRenseignee;
import com.immo.gestion.utilisateur.domain.DonneesNaissanceRenseignees;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateurComplete;
import com.immo.gestion.utilisateur.domain.StatutProfil;
import com.immo.gestion.utilisateur.domain.TelephoneRenseigne;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maintient la projection {@code utilisateurs} à jour à partir des événements de l'event store.
 * {@code @EventListener} volontairement synchrone (pas {@code @TransactionalEventListener}) pour
 * s'exécuter dans la même transaction que l'append. Cf. MISSION.md §5.
 */
@Component
public class UtilisateurProjectionListener {

    private final UtilisateurJpaRepository jpa;

    public UtilisateurProjectionListener(UtilisateurJpaRepository jpa) {
        this.jpa = jpa;
    }

    @EventListener
    public void surInscription(UtilisateurInscrit evenement) {
        jpa.save(new UtilisateurEntity(
                evenement.utilisateurId().valeur(),
                evenement.email().valeur(),
                evenement.hashMotDePasse().phcEncoded(),
                evenement.nom(),
                evenement.prenom(),
                evenement.telephone(),
                evenement.statut(),
                evenement.versionCgu(),
                evenement.cguAccepteesLe(),
                evenement.versionConfidentialite(),
                evenement.confidentialiteAccepteeLe(),
                evenement.survenuLe(),
                null, null, null, null, null,
                null, null, null, null, null, null,
                StatutProfil.MINIMAL,
                null
        ));
    }

    @EventListener
    public void surCivilite(CiviliteRenseignee evenement) {
        UtilisateurEntity e = charger(evenement.utilisateurId());
        e.setCivilite(evenement.civilite());
        jpa.save(e);
    }

    @EventListener
    public void surDonneesNaissance(DonneesNaissanceRenseignees evenement) {
        UtilisateurEntity e = charger(evenement.utilisateurId());
        e.setDateNaissance(evenement.dateNaissance());
        e.setLieuNaissanceVille(evenement.lieuNaissanceVille());
        e.setLieuNaissancePaysIso(evenement.lieuNaissancePaysIso());
        e.setNationaliteIso(evenement.nationaliteIso());
        jpa.save(e);
    }

    @EventListener
    public void surAdresseDomicile(AdresseDomicileRenseignee evenement) {
        UtilisateurEntity e = charger(evenement.utilisateurId());
        Adresse adresse = evenement.adresseDomicile();
        e.setAdresseNumero(adresse.numero());
        e.setAdresseVoie(adresse.voie());
        e.setAdresseComplement(adresse.complement());
        e.setAdresseCodePostal(adresse.codePostal());
        e.setAdresseCommune(adresse.commune());
        e.setAdressePaysIso(adresse.paysIso());
        jpa.save(e);
    }

    @EventListener
    public void surTelephone(TelephoneRenseigne evenement) {
        UtilisateurEntity e = charger(evenement.utilisateurId());
        e.setTelephone(evenement.telephone());
        jpa.save(e);
    }

    @EventListener
    public void surProfilComplet(ProfilUtilisateurComplete evenement) {
        UtilisateurEntity e = charger(evenement.utilisateurId());
        e.setStatutProfil(StatutProfil.COMPLET);
        e.setProfilCompleteLe(evenement.survenuLe());
        jpa.save(e);
    }

    private UtilisateurEntity charger(UtilisateurId id) {
        UUID valeur = id.valeur();
        return jpa.findById(valeur)
                .orElseThrow(() -> new IllegalStateException(
                        "Projection utilisateurs incohérente : utilisateur introuvable " + valeur));
    }
}

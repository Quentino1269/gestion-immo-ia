package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.domain.Adresse;
import com.immo.gestion.utilisateur.domain.StatutProfil;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UtilisateurRepositoryAdapter implements UtilisateurRepository {

    private final UtilisateurJpaRepository jpa;

    public UtilisateurRepositoryAdapter(UtilisateurJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existeParEmail(Email email) {
        return jpa.existsByEmail(email.valeur());
    }

    @Override
    public void enregistrer(Utilisateur u) {
        Adresse adr = u.adresseDomicile();
        jpa.save(new UtilisateurEntity(
                u.id().valeur(),
                u.email().valeur(),
                u.hashMotDePasse().phcEncoded(),
                u.nom(),
                u.prenom(),
                u.telephone(),
                u.statut(),
                u.versionCgu(),
                u.cguAccepteesLe(),
                u.versionConfidentialite(),
                u.confidentialiteAccepteeLe(),
                u.inscritLe(),
                u.civilite(),
                u.dateNaissance(),
                u.lieuNaissanceVille(),
                u.lieuNaissancePaysIso(),
                u.nationaliteIso(),
                adr != null ? adr.numero() : null,
                adr != null ? adr.voie() : null,
                adr != null ? adr.complement() : null,
                adr != null ? adr.codePostal() : null,
                adr != null ? adr.commune() : null,
                adr != null ? adr.paysIso() : null,
                u.statutProfil(),
                u.profilCompleteLe()
        ));
    }

    @Override
    public Optional<Utilisateur> chargerParId(UtilisateurId id) {
        return jpa.findById(id.valeur()).map(this::versDomaine);
    }

    private Utilisateur versDomaine(UtilisateurEntity e) {
        Adresse adresse = null;
        if (e.getAdresseNumero() != null && e.getAdresseVoie() != null) {
            adresse = new Adresse(
                    e.getAdresseNumero(),
                    e.getAdresseVoie(),
                    e.getAdresseComplement(),
                    e.getAdresseCodePostal(),
                    e.getAdresseCommune(),
                    e.getAdressePaysIso()
            );
        }
        return new Utilisateur(
                new UtilisateurId(e.getId()),
                new Email(e.getEmail()),
                new HashMotDePasse(e.getHashMotDePasse()),
                e.getNom(),
                e.getPrenom(),
                e.getTelephone(),
                e.getStatut(),
                e.getVersionCgu(),
                e.getCguAccepteesLe(),
                e.getVersionConfidentialite(),
                e.getConfidentialiteAccepteeLe(),
                e.getInscritLe(),
                e.getCivilite(),
                e.getDateNaissance(),
                e.getLieuNaissanceVille(),
                e.getLieuNaissancePaysIso(),
                e.getNationaliteIso(),
                adresse,
                e.getStatutProfil() != null ? e.getStatutProfil() : StatutProfil.MINIMAL,
                e.getProfilCompleteLe()
        );
    }
}

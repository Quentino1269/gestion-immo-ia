package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
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
                u.inscritLe()
        ));
    }

    @Override
    public Optional<Utilisateur> chargerParId(UtilisateurId id) {
        return jpa.findById(id.valeur()).map(this::versDomaine);
    }

    private Utilisateur versDomaine(UtilisateurEntity e) {
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
                e.getInscritLe()
        );
    }
}

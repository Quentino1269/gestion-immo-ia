package com.immo.gestion.utilisateur.adapter.persistence;

import com.immo.gestion.shared.Adresse;
import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.domain.StatutProfil;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Lecture adossée à la projection {@code utilisateurs}, maintenue à jour par {@link UtilisateurProjectionListener}.
 */
@Repository
public class UtilisateurQueryRepositoryAdapter implements UtilisateurQueryRepository {

    private final UtilisateurJpaRepository jpa;

    public UtilisateurQueryRepositoryAdapter(UtilisateurJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existeParEmail(Email email) {
        return jpa.existsByEmail(email.valeur());
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

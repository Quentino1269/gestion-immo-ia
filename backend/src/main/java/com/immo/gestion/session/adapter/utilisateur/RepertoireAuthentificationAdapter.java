package com.immo.gestion.session.adapter.utilisateur;

import com.immo.gestion.session.domain.port.out.InfosAuthentification;
import com.immo.gestion.session.domain.port.out.RepertoireAuthentification;
import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.utilisateur.adapter.persistence.UtilisateurJpaRepository;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implémentation du port {@link RepertoireAuthentification} qui consulte la
 * persistance utilisateur en lecture seule. En V1 il n'existe pas de read model
 * matérialisé séparé : l'index {@code (email → utilisateurId, hash, statut)}
 * est porté par la table {@code utilisateurs} (slice creation-utilisateur, V1
 * sans event store — cf. MISSION §5).
 */
@Component
public class RepertoireAuthentificationAdapter implements RepertoireAuthentification {

    private final UtilisateurJpaRepository utilisateurs;

    public RepertoireAuthentificationAdapter(UtilisateurJpaRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public Optional<InfosAuthentification> chercherParEmail(Email email) {
        return utilisateurs.findByEmail(email.valeur()).map(e -> new InfosAuthentification(
                new UtilisateurId(e.getId()),
                new HashMotDePasse(e.getHashMotDePasse()),
                e.getStatut()
        ));
    }
}

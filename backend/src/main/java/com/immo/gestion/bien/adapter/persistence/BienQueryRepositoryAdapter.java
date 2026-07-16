package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lecture adossée à la projection {@code biens}, maintenue à jour par {@link BienProjectionListener}.
 */
@Repository
public class BienQueryRepositoryAdapter implements BienQueryRepository {

    private final BienJpaRepository jpa;

    public BienQueryRepositoryAdapter(BienJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Bien> chargerParId(BienId id) {
        return jpa.findById(id.valeur()).map(this::versDomaine);
    }

    @Override
    public List<Bien> chargerParProprietaire(UtilisateurId proprietaireId) {
        return jpa.findByProprietaireInitialId(proprietaireId.valeur())
                .stream()
                .map(this::versDomaine)
                .toList();
    }

    @Override
    public List<Bien> chargerChambresParParent(BienId parentId) {
        return jpa.findByBienParentId(parentId.valeur())
                .stream()
                .map(this::versDomaine)
                .toList();
    }

    private Bien versDomaine(BienEntity e) {
        UUID parentUuid = e.getBienParentId();
        return new Bien(
                new BienId(e.getId()),
                new UtilisateurId(e.getProprietaireInitialId()),
                e.getTypeBien(),
                parentUuid != null ? new BienId(parentUuid) : null,
                e.getLibelleChambre(),
                e.getNbPiecesPrincipales(),
                e.getSurfaceM2(),
                e.isMeuble(),
                e.getLoyerHorsChargesEnCentimes(),
                e.getChargesEnCentimes(),
                e.getModaliteCharges(),
                new Adresse(
                        e.getAdresseNumero(),
                        e.getAdresseVoie(),
                        e.getAdresseComplement(),
                        e.getAdresseCodePostal(),
                        e.getAdresseCommune(),
                        e.getAdressePaysIso()
                ),
                e.getDisponibleAPartirDu(),
                e.getAjouteLe()
        );
    }
}

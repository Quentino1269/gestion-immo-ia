package com.immo.gestion.bien.adapter.persistence;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BienRepositoryAdapter implements BienRepository {

    private final BienJpaRepository jpa;

    public BienRepositoryAdapter(BienJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void enregistrer(Bien bien) {
        Adresse adr = bien.adresse();
        jpa.save(new BienEntity(
                bien.id().valeur(),
                bien.proprietaireInitialId().valeur(),
                bien.typeBien(),
                bien.bienParentId() != null ? bien.bienParentId().valeur() : null,
                bien.libelleChambre(),
                bien.nbPiecesPrincipales(),
                bien.surfaceM2(),
                bien.meuble(),
                bien.loyerHorsChargesEnCentimes(),
                bien.chargesEnCentimes(),
                bien.modaliteCharges(),
                adr.numero(),
                adr.voie(),
                adr.complement(),
                adr.codePostal(),
                adr.commune(),
                adr.paysIso(),
                bien.disponibleAPartirDu(),
                bien.ajouteLe()
        ));
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

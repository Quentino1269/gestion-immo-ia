package com.immo.gestion.bien.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.bien.domain.port.in.BienNonTrouveException;
import com.immo.gestion.bien.domain.port.in.BienParentIntrouvableException;
import com.immo.gestion.bien.domain.port.in.CreerBienCommand;
import com.immo.gestion.bien.domain.port.in.CreerBienUseCase;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurParentException;
import com.immo.gestion.bien.domain.port.in.LibelleChambreNonUniqueException;
import com.immo.gestion.bien.domain.port.in.ObtenirFicheBienUseCase;
import com.immo.gestion.bien.domain.port.in.ObtenirMonPortefeuilleUseCase;
import com.immo.gestion.bien.domain.port.in.SurfaceChambresDepasseeException;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class BienService implements CreerBienUseCase, ObtenirMonPortefeuilleUseCase, ObtenirFicheBienUseCase {

    private final BienRepository repository;
    private final BienQueryRepository queryRepository;
    private final Clock clock;

    public BienService(BienRepository repository, BienQueryRepository queryRepository, Clock clock) {
        this.repository = repository;
        this.queryRepository = queryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FicheBien creer(CreerBienCommand commande) {
        if (commande.bienParentId() != null) {
            verifierInvariantsColocation(commande);
        }

        Instant maintenant = Instant.now(clock);
        Bien bien = new Bien(
                BienId.nouveau(),
                commande.proprietaireId(),
                commande.typeBien(),
                commande.bienParentId(),
                commande.libelleChambre(),
                commande.nbPiecesPrincipales(),
                commande.surfaceM2(),
                commande.meuble(),
                commande.loyerHorsChargesEnCentimes(),
                commande.chargesEnCentimes(),
                commande.modaliteCharges(),
                commande.adresse(),
                commande.disponibleAPartirDu(),
                maintenant
        );

        DomainEvent evenement = BienAjouteAuPortefeuille.depuis(bien);
        repository.enregistrer(bien.id(), 0L, List.of(evenement));

        return FicheBien.depuis(bien);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LignePortefeuille> obtenir(UtilisateurId proprietaireId) {
        return queryRepository.chargerParProprietaire(proprietaireId)
                .stream()
                .map(LignePortefeuille::depuis)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FicheBien obtenir(BienId bienId, UtilisateurId demandeurId) {
        Bien bien = queryRepository.chargerParId(bienId)
                .orElseThrow(() -> new BienNonTrouveException(bienId));
        // V1 mono-propriétaire (D16, cf. docs/slices/creation-bien.md) : ayant droit = propriétaire initial.
        if (!bien.proprietaireInitialId().equals(demandeurId)) {
            throw new DroitInsuffisantSurBienException();
        }
        return FicheBien.depuis(bien);
    }

    // --- invariants cross-aggregate (I-COLOC-2, I-COLOC-4, I-COLOC-5) ---

    private void verifierInvariantsColocation(CreerBienCommand commande) {
        Bien parent = queryRepository.chargerParId(commande.bienParentId())
                .orElseThrow(() -> new BienParentIntrouvableException(commande.bienParentId()));

        // I-COLOC-2 : le demandeur est propriétaire du parent
        if (!parent.proprietaireInitialId().equals(commande.proprietaireId())) {
            throw new DroitInsuffisantSurParentException();
        }

        List<Bien> chambresExistantes = queryRepository.chargerChambresParParent(commande.bienParentId());

        // I-COLOC-5 : libellé unique dans le parent
        boolean libelleExiste = chambresExistantes.stream()
                .anyMatch(c -> commande.libelleChambre() != null
                        && commande.libelleChambre().strip().equalsIgnoreCase(
                                c.libelleChambre() != null ? c.libelleChambre().strip() : ""));
        if (libelleExiste) {
            throw new LibelleChambreNonUniqueException(commande.libelleChambre());
        }

        // I-COLOC-4 : somme des surfaces ≤ surface parent
        BigDecimal sommeChambresSurface = chambresExistantes.stream()
                .map(Bien::surfaceM2)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(commande.surfaceM2());
        if (sommeChambresSurface.compareTo(parent.surfaceM2()) > 0) {
            throw new SurfaceChambresDepasseeException();
        }
    }
}

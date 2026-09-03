package com.immo.gestion.bien.application;

import com.immo.gestion.bien.domain.Bien;
import com.immo.gestion.bien.domain.BienAjouteAuPortefeuille;
import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.ChargesRevisees;
import com.immo.gestion.bien.domain.DisponibiliteRevisee;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.LibelleChambreRenomme;
import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.bien.domain.LogementDevenuNu;
import com.immo.gestion.bien.domain.LoyerRevise;
import com.immo.gestion.bien.domain.MeubleEntreDansLeLogement;
import com.immo.gestion.bien.domain.ModaliteCharges;
import com.immo.gestion.bien.domain.NombrePiecesRevise;
import com.immo.gestion.bien.domain.TypeBien;
import com.immo.gestion.bien.domain.port.in.BienNonTrouveException;
import com.immo.gestion.bien.domain.port.in.BienParentIntrouvableException;
import com.immo.gestion.bien.domain.port.in.CreerBienCommand;
import com.immo.gestion.bien.domain.port.in.CreerBienUseCase;
import com.immo.gestion.shared.domain.port.in.DroitInsuffisantSurBienException;
import com.immo.gestion.bien.domain.port.in.DroitInsuffisantSurParentException;
import com.immo.gestion.bien.domain.port.in.LibelleChambreNonUniqueException;
import com.immo.gestion.bien.domain.port.in.ModifierBienCommand;
import com.immo.gestion.bien.domain.port.in.ModifierBienUseCase;
import com.immo.gestion.bien.domain.port.in.ObtenirFicheBienUseCase;
import com.immo.gestion.bien.domain.port.in.ObtenirMonPortefeuilleUseCase;
import com.immo.gestion.bien.domain.port.in.SurfaceChambresDepasseeException;
import com.immo.gestion.bien.domain.port.out.BienQueryRepository;
import com.immo.gestion.bien.domain.port.out.BienRepository;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BienService implements CreerBienUseCase, ObtenirMonPortefeuilleUseCase, ObtenirFicheBienUseCase,
        ModifierBienUseCase {

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
    @Transactional
    public FicheBien modifier(ModifierBienCommand commande) {
        EtatCharge<Bien> etatCharge = repository.chargerParId(commande.bienId())
                .orElseThrow(() -> new BienNonTrouveException(commande.bienId()));
        Bien existante = etatCharge.aggregat();

        // I-MOD-2
        if (!existante.proprietaireInitialId().equals(commande.demandeurId())) {
            throw new DroitInsuffisantSurBienException();
        }

        ModaliteCharges modaliteCharges = commande.meuble() ? ModaliteCharges.FORFAIT : ModaliteCharges.PROVISION;
        // Réutilise le constructeur canonique de Bien pour revalider tous les invariants de champ
        // (I-MOD-3..5, I-MOD-7, I-MOD-9) sans les dupliquer ici.
        Bien candidat = new Bien(
                existante.id(), existante.proprietaireInitialId(), existante.typeBien(), existante.bienParentId(),
                commande.libelleChambre(), commande.nbPiecesPrincipales(), existante.surfaceM2(), commande.meuble(),
                commande.loyerHorsChargesEnCentimes(), commande.chargesEnCentimes(), modaliteCharges,
                existante.adresse(), commande.disponibleAPartirDu(), existante.ajouteLe()
        );

        // I-MOD-6 : libellé unique parmi les *autres* chambres du même parent
        if (candidat.typeBien() == TypeBien.CHAMBRE_COLOCATION
                && !Objects.equals(candidat.libelleChambre(), existante.libelleChambre())) {
            boolean dupliquee = queryRepository.chargerChambresParParent(existante.bienParentId()).stream()
                    .filter(c -> !c.id().equals(existante.id()))
                    .anyMatch(c -> candidat.libelleChambre().equalsIgnoreCase(
                            c.libelleChambre() != null ? c.libelleChambre().strip() : ""));
            if (dupliquee) {
                throw new LibelleChambreNonUniqueException(candidat.libelleChambre());
            }
        }

        Instant maintenant = Instant.now(clock);
        List<DomainEvent> evenements = new ArrayList<>();
        if (candidat.loyerHorsChargesEnCentimes() != existante.loyerHorsChargesEnCentimes()) {
            evenements.add(new LoyerRevise(existante.id(), candidat.loyerHorsChargesEnCentimes(), maintenant));
        }
        if (candidat.chargesEnCentimes() != existante.chargesEnCentimes()) {
            evenements.add(new ChargesRevisees(existante.id(), candidat.chargesEnCentimes(), maintenant));
        }
        if (candidat.meuble() != existante.meuble()) {
            evenements.add(candidat.meuble()
                    ? new MeubleEntreDansLeLogement(existante.id(), candidat.modaliteCharges(), maintenant)
                    : new LogementDevenuNu(existante.id(), candidat.modaliteCharges(), maintenant));
        }
        if (!candidat.disponibleAPartirDu().equals(existante.disponibleAPartirDu())) {
            evenements.add(new DisponibiliteRevisee(existante.id(), candidat.disponibleAPartirDu(), maintenant));
        }
        if (!Objects.equals(candidat.libelleChambre(), existante.libelleChambre())) {
            evenements.add(new LibelleChambreRenomme(existante.id(), candidat.libelleChambre(), maintenant));
        }
        if (candidat.nbPiecesPrincipales() != existante.nbPiecesPrincipales()) {
            evenements.add(new NombrePiecesRevise(existante.id(), candidat.nbPiecesPrincipales(), maintenant));
        }

        // I-MOD-8 (D4) : no-op silencieux si rien n'a changé
        if (evenements.isEmpty()) {
            return FicheBien.depuis(existante);
        }

        repository.enregistrer(existante.id(), etatCharge.version(), evenements);
        return FicheBien.depuis(candidat);
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

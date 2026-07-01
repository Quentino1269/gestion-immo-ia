package com.immo.gestion.bien.adapter.web;

import com.immo.gestion.bien.domain.BienId;
import com.immo.gestion.bien.domain.FicheBien;
import com.immo.gestion.bien.domain.LignePortefeuille;
import com.immo.gestion.bien.domain.port.in.CreerBienCommand;
import com.immo.gestion.bien.domain.port.in.CreerBienUseCase;
import com.immo.gestion.bien.domain.port.in.ObtenirFicheBienUseCase;
import com.immo.gestion.bien.domain.port.in.ObtenirMonPortefeuilleUseCase;
import com.immo.gestion.session.adapter.web.ContexteAuthentificationRequete;
import com.immo.gestion.shared.Adresse;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/biens")
public class BienController {

    private final CreerBienUseCase creer;
    private final ObtenirMonPortefeuilleUseCase portefeuille;
    private final ObtenirFicheBienUseCase ficheBien;

    public BienController(
            CreerBienUseCase creer,
            ObtenirMonPortefeuilleUseCase portefeuille,
            ObtenirFicheBienUseCase ficheBien
    ) {
        this.creer = creer;
        this.portefeuille = portefeuille;
        this.ficheBien = ficheBien;
    }

    @PostMapping
    public ResponseEntity<FicheBienResponse> creerBien(
            @RequestBody CreerBienRequest body,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CreerBienRequest.AdresseRequest a = body.adresse();
        Adresse adresse = a != null
                ? new Adresse(a.numero(), a.voie(), a.complement(), a.codePostal(), a.commune(), a.paysIso())
                : null;

        FicheBien fiche = creer.creer(new CreerBienCommand(
                uid,
                body.typeBien(),
                body.bienParentId() != null ? new BienId(body.bienParentId()) : null,
                body.libelleChambre(),
                body.nbPiecesPrincipales(),
                body.surfaceM2(),
                body.meuble(),
                body.loyerHorsChargesEnCentimes(),
                body.chargesEnCentimes(),
                body.modaliteCharges(),
                adresse,
                body.disponibleAPartirDu()
        ));

        URI emplacement = UriComponentsBuilder.fromPath("/api/biens/{id}")
                .buildAndExpand(fiche.bienId().valeur())
                .toUri();
        return ResponseEntity.created(emplacement).body(FicheBienResponse.depuis(fiche));
    }

    @GetMapping
    public ResponseEntity<List<LignePortefeuilleResponse>> obtenirPortefeuille(HttpServletRequest requete) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<LignePortefeuille> lignes = portefeuille.obtenir(uid);
        return ResponseEntity.ok(lignes.stream().map(LignePortefeuilleResponse::depuis).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FicheBienResponse> obtenirFiche(
            @PathVariable UUID id,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete).orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        FicheBien fiche = ficheBien.obtenir(new BienId(id), uid);
        return ResponseEntity.ok(FicheBienResponse.depuis(fiche));
    }
}

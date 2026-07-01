package com.immo.gestion.utilisateur.adapter.web;

import com.immo.gestion.session.adapter.web.ContexteAuthentificationRequete;
import com.immo.gestion.utilisateur.domain.Adresse;
import com.immo.gestion.utilisateur.domain.ProfilUtilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.in.CompleterMonProfilCivilCommand;
import com.immo.gestion.utilisateur.domain.port.in.CompleterMonProfilCivilUseCase;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurCommand;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurUseCase;
import com.immo.gestion.utilisateur.domain.port.in.ObtenirMonProfilUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final CreerUtilisateurUseCase creer;
    private final CompleterMonProfilCivilUseCase completerProfil;
    private final ObtenirMonProfilUseCase obtenirProfil;

    public UtilisateurController(
            CreerUtilisateurUseCase creer,
            CompleterMonProfilCivilUseCase completerProfil,
            ObtenirMonProfilUseCase obtenirProfil
    ) {
        this.creer = creer;
        this.completerProfil = completerProfil;
        this.obtenirProfil = obtenirProfil;
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponse> inscrire(@Valid @RequestBody CreerUtilisateurRequest body) {
        UtilisateurId id = creer.creer(new CreerUtilisateurCommand(
                body.email(),
                body.motDePasse(),
                body.nom(),
                body.prenom(),
                body.telephone(),
                body.accepteCgu(),
                body.accepteConfidentialite()
        ));
        URI emplacement = UriComponentsBuilder.fromPath("/api/utilisateurs/{id}")
                .buildAndExpand(id.valeur())
                .toUri();
        return ResponseEntity.created(emplacement).body(new UtilisateurResponse(id.valeur()));
    }

    @GetMapping("/moi/profil")
    public ResponseEntity<ProfilResponse> obtenirMonProfil(HttpServletRequest requete) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete)
                .orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ProfilUtilisateur profil = obtenirProfil.obtenir(uid);
        return ResponseEntity.ok(ProfilResponse.depuis(profil));
    }

    @PutMapping("/moi/profil")
    public ResponseEntity<ProfilResponse> completerMonProfil(
            @RequestBody CompleterProfilRequest body,
            HttpServletRequest requete
    ) {
        UtilisateurId uid = ContexteAuthentificationRequete.utilisateurCourant(requete)
                .orElse(null);
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Adresse adresse = null;
        if (body.adresseDomicile() != null) {
            AdresseRequest a = body.adresseDomicile();
            adresse = new Adresse(a.numero(), a.voie(), a.complement(),
                    a.codePostal(), a.commune(), a.paysIso());
        }

        CompleterMonProfilCivilCommand commande = new CompleterMonProfilCivilCommand(
                uid,
                body.civilite(),
                body.dateNaissance(),
                body.lieuNaissanceVille(),
                body.lieuNaissancePaysIso(),
                body.nationaliteIso(),
                adresse,
                body.telephone()
        );

        ProfilUtilisateur profil = completerProfil.completer(commande);
        return ResponseEntity.ok(ProfilResponse.depuis(profil));
    }
}

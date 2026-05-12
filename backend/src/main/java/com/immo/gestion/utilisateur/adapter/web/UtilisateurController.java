package com.immo.gestion.utilisateur.adapter.web;

import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurCommand;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final CreerUtilisateurUseCase creer;

    public UtilisateurController(CreerUtilisateurUseCase creer) {
        this.creer = creer;
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
}

package com.immo.gestion.session.adapter.web;

import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.port.in.SeConnecterCommand;
import com.immo.gestion.session.domain.port.in.SeConnecterResultat;
import com.immo.gestion.session.domain.port.in.SeConnecterUseCase;
import com.immo.gestion.session.domain.port.in.SeDeconnecterCommand;
import com.immo.gestion.session.domain.port.in.SeDeconnecterUseCase;
import com.immo.gestion.session.domain.port.in.SessionInvalideException;
import com.immo.gestion.shared.MotDePasseSoumis;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SeConnecterUseCase seConnecter;
    private final SeDeconnecterUseCase seDeconnecter;

    public SessionController(SeConnecterUseCase seConnecter, SeDeconnecterUseCase seDeconnecter) {
        this.seConnecter = seConnecter;
        this.seDeconnecter = seDeconnecter;
    }

    @PostMapping
    public ResponseEntity<SeConnecterResponse> ouvrir(
            @Valid @RequestBody SeConnecterRequest body,
            HttpServletRequest requete
    ) {
        SeConnecterResultat resultat = seConnecter.seConnecter(new SeConnecterCommand(
                body.email(),
                new MotDePasseSoumis(body.motDePasse()),
                requete.getHeader(HttpHeaders.USER_AGENT),
                requete.getRemoteAddr()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(new SeConnecterResponse(
                resultat.sessionId().valeur(),
                resultat.tokenClair().valeur(),
                resultat.expireA()
        ));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> fermer(
            @PathVariable("sessionId") UUID sessionId,
            HttpServletRequest requete
    ) {
        UtilisateurId appelant = ContexteAuthentificationRequete.utilisateurCourant(requete)
                .orElseThrow(SessionInvalideException::new);
        seDeconnecter.seDeconnecter(new SeDeconnecterCommand(new SessionId(sessionId), appelant));
        return ResponseEntity.noContent().build();
    }
}

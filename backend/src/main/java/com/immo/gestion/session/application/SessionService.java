package com.immo.gestion.session.application;

import com.immo.gestion.session.domain.MotifFermeture;
import com.immo.gestion.session.domain.RaisonEchecConnexion;
import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.SessionId;
import com.immo.gestion.session.domain.TentativeDeConnexionEchouee;
import com.immo.gestion.session.domain.TokenSessionGenere;
import com.immo.gestion.session.domain.UtilisateurConnecte;
import com.immo.gestion.session.domain.UtilisateurDeconnecte;
import com.immo.gestion.session.domain.port.in.IdentifiantsInvalidesException;
import com.immo.gestion.session.domain.port.in.SeConnecterCommand;
import com.immo.gestion.session.domain.port.in.SeConnecterResultat;
import com.immo.gestion.session.domain.port.in.SeConnecterUseCase;
import com.immo.gestion.session.domain.port.in.SeDeconnecterCommand;
import com.immo.gestion.session.domain.port.in.SeDeconnecterUseCase;
import com.immo.gestion.session.domain.port.in.SessionInvalideException;
import com.immo.gestion.session.domain.port.out.GenerateurTokenSession;
import com.immo.gestion.session.domain.port.out.InfosAuthentification;
import com.immo.gestion.session.domain.port.out.RepertoireAuthentification;
import com.immo.gestion.session.domain.port.out.SessionRepository;
import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseSoumis;
import com.immo.gestion.shared.domain.DomainEvent;
import com.immo.gestion.shared.domain.EtatCharge;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService implements SeConnecterUseCase, SeDeconnecterUseCase {

    private final RepertoireAuthentification repertoire;
    private final HasheurMotDePasse hasheur;
    private final HashMotDePasse hashDummy;
    private final GenerateurTokenSession generateurToken;
    private final SessionRepository sessions;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration dureeSession;

    public SessionService(
            RepertoireAuthentification repertoire,
            HasheurMotDePasse hasheur,
            HashMotDePasse hashDummyAntiEnumeration,
            GenerateurTokenSession generateurToken,
            SessionRepository sessions,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            Duration dureeSession
    ) {
        this.repertoire = repertoire;
        this.hasheur = hasheur;
        this.hashDummy = hashDummyAntiEnumeration;
        this.generateurToken = generateurToken;
        this.sessions = sessions;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.dureeSession = dureeSession;
    }

    @Override
    @Transactional
    public SeConnecterResultat seConnecter(SeConnecterCommand commande) {
        MotDePasseSoumis soumission = commande.motDePasseSoumis();
        try {
            Email email = parserEmailOuEchouer(commande, soumission);

            Optional<InfosAuthentification> infos = repertoire.chercherParEmail(email);
            if (infos.isEmpty()) {
                // Anti-énumération (D7, I-2) : exécution d'un argon2id factice
                // pour aligner le temps de réponse sur le chemin "email connu".
                hasheur.verifierSoumission(hashDummy, soumission);
                publierEchec(commande, RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
                throw new IdentifiantsInvalidesException();
            }

            InfosAuthentification i = infos.get();
            boolean motDePasseValide = hasheur.verifierSoumission(i.hashMotDePasse(), soumission);
            if (!motDePasseValide) {
                publierEchec(commande, RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
                throw new IdentifiantsInvalidesException();
            }
            if (i.statut() != StatutCompte.ACTIF) {
                publierEchec(commande, RaisonEchecConnexion.COMPTE_INACTIF);
                throw new IdentifiantsInvalidesException();
            }

            Instant maintenant = Instant.now(clock);
            TokenSessionGenere token = generateurToken.genererNouveau();
            Session session = Session.ouvrir(
                    i.utilisateurId(),
                    token.hash(),
                    maintenant,
                    dureeSession,
                    commande.userAgent(),
                    commande.ipSource()
            );
            DomainEvent evenement = new UtilisateurConnecte(
                    session.id(),
                    session.utilisateurId(),
                    session.tokenHash(),
                    session.expireA(),
                    session.userAgentOpt().orElse(null),
                    session.ipSourceOpt().orElse(null),
                    maintenant
            );
            sessions.enregistrer(session.id(), 0L, List.of(evenement));
            return new SeConnecterResultat(session.id(), token.clair(), session.expireA());
        } finally {
            soumission.effacer();
        }
    }

    @Override
    @Transactional
    public void seDeconnecter(SeDeconnecterCommand commande) {
        SessionId sessionId = commande.sessionId();
        EtatCharge<Session> etatCharge = sessions.chargerParId(sessionId)
                .orElseThrow(SessionInvalideException::new);
        Session session = etatCharge.aggregat();
        // I-6 : seul le propriétaire peut fermer sa session
        if (!session.utilisateurId().equals(commande.utilisateurAppelant())) {
            throw new SessionInvalideException();
        }
        // I-7 : pas de réactivation d'une session déjà fermée
        if (session.etat() != com.immo.gestion.session.domain.EtatSession.ACTIVE) {
            throw new SessionInvalideException();
        }
        Instant maintenant = Instant.now(clock);
        Session fermee = session.fermer(MotifFermeture.VOLONTAIRE, maintenant);
        DomainEvent evenement = new UtilisateurDeconnecte(
                fermee.id(),
                fermee.utilisateurId(),
                MotifFermeture.VOLONTAIRE,
                maintenant
        );
        sessions.enregistrer(sessionId, etatCharge.version(), List.of(evenement));
    }

    /**
     * Email mal formé = échec d'identifiants côté API (D7). On émet un event
     * d'audit avec {@code IDENTIFIANTS_INVALIDES} et on exécute le dummy hash
     * pour préserver le timing constant.
     */
    private Email parserEmailOuEchouer(SeConnecterCommand commande, MotDePasseSoumis soumission) {
        try {
            return new Email(commande.email());
        } catch (RuntimeException invalide) {
            hasheur.verifierSoumission(hashDummy, soumission);
            publierEchec(commande, RaisonEchecConnexion.IDENTIFIANTS_INVALIDES);
            throw new IdentifiantsInvalidesException();
        }
    }

    private void publierEchec(SeConnecterCommand commande, RaisonEchecConnexion raison) {
        String emailNormalise = normaliserEmailPourAudit(commande.email());
        eventPublisher.publishEvent(new TentativeDeConnexionEchouee(
                emailNormalise,
                raison,
                commande.userAgent(),
                commande.ipSource(),
                Instant.now(clock)
        ));
    }

    private static String normaliserEmailPourAudit(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}

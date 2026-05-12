package com.immo.gestion.utilisateur.application;

import com.immo.gestion.shared.Email;
import com.immo.gestion.shared.HashMotDePasse;
import com.immo.gestion.shared.MotDePasseClair;
import com.immo.gestion.utilisateur.config.ConsentementsActuels;
import com.immo.gestion.utilisateur.domain.StatutCompte;
import com.immo.gestion.utilisateur.domain.Utilisateur;
import com.immo.gestion.utilisateur.domain.UtilisateurId;
import com.immo.gestion.utilisateur.domain.UtilisateurInscrit;
import com.immo.gestion.utilisateur.domain.port.in.ConsentementsNonAcceptesException;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurCommand;
import com.immo.gestion.utilisateur.domain.port.in.CreerUtilisateurUseCase;
import com.immo.gestion.utilisateur.domain.port.in.EmailDejaUtiliseException;
import com.immo.gestion.utilisateur.domain.port.out.HasheurMotDePasse;
import com.immo.gestion.utilisateur.domain.port.out.UtilisateurRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class UtilisateurService implements CreerUtilisateurUseCase {

    private final UtilisateurRepository repository;
    private final HasheurMotDePasse hasheur;
    private final ConsentementsActuels consentements;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public UtilisateurService(
            UtilisateurRepository repository,
            HasheurMotDePasse hasheur,
            ConsentementsActuels consentements,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = repository;
        this.hasheur = hasheur;
        this.consentements = consentements;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UtilisateurId creer(CreerUtilisateurCommand commande) {
        // I-5 : consentements obligatoires
        if (!commande.accepteCgu() || !commande.accepteConfidentialite()) {
            throw new ConsentementsNonAcceptesException();
        }

        // Construction des VO (lève IllegalArgumentException si invariants violés)
        Email email = new Email(commande.email());

        // I-2 : unicité email
        if (repository.existeParEmail(email)) {
            throw new EmailDejaUtiliseException();
        }

        MotDePasseClair motDePasse = new MotDePasseClair(commande.motDePasseClair());
        HashMotDePasse hash = hasheur.hasher(motDePasse);

        Instant maintenant = Instant.now(clock);
        UtilisateurId id = UtilisateurId.nouveau();

        Utilisateur utilisateur = new Utilisateur(
                id,
                email,
                hash,
                commande.nom(),
                commande.prenom(),
                commande.telephone(),
                StatutCompte.ACTIF, // D3
                consentements.versionCgu(),
                maintenant,
                consentements.versionConfidentialite(),
                maintenant,
                maintenant
        );

        repository.enregistrer(utilisateur);
        eventPublisher.publishEvent(UtilisateurInscrit.depuis(utilisateur));

        return id;
    }
}

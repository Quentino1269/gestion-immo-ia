package com.immo.gestion.session.adapter.web;

import com.immo.gestion.session.domain.Session;
import com.immo.gestion.session.domain.TokenSession;
import com.immo.gestion.session.domain.TokenSessionHash;
import com.immo.gestion.session.domain.port.out.GenerateurTokenSession;
import com.immo.gestion.session.domain.port.out.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Filter d'authentification sur mesure (V1 sans Spring Security, cf. MISSION §5).
 * Lit {@code Authorization: Bearer <token>}, résout le hash sha-256, charge la
 * {@link Session}, vérifie qu'elle est active et non expirée, puis pose
 * {@code utilisateurId} et {@code sessionId} en attributs de requête.
 * <p>
 * Le filter ne renvoie <b>jamais</b> de 401 lui-même : il se contente de poser
 * (ou non) le contexte. Les controllers protégés rejettent la requête s'ils ne
 * trouvent pas le contexte. Cela évite de bloquer les endpoints publics
 * (POST {@code /api/utilisateurs}, POST {@code /api/sessions}) et garde le
 * contrat anti-énumération inchangé côté login.
 */
@Component
public class BearerAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE_BEARER = "Bearer ";

    private final GenerateurTokenSession hasheurToken;
    private final SessionRepository sessions;
    private final Clock clock;

    public BearerAuthFilter(
            GenerateurTokenSession hasheurToken,
            SessionRepository sessions,
            Clock clock
    ) {
        this.hasheurToken = hasheurToken;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        extraireToken(request)
                .flatMap(this::resoudreSession)
                .filter(session -> session.estActive(Instant.now(clock)))
                .ifPresent(session -> {
                    request.setAttribute(
                            ContexteAuthentificationRequete.ATTR_UTILISATEUR_ID,
                            session.utilisateurId().valeur()
                    );
                    request.setAttribute(
                            ContexteAuthentificationRequete.ATTR_SESSION_ID,
                            session.id().valeur()
                    );
                });
        filterChain.doFilter(request, response);
    }

    private static Optional<TokenSession> extraireToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIXE_BEARER)) {
            return Optional.empty();
        }
        String valeur = header.substring(PREFIXE_BEARER.length()).trim();
        if (valeur.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TokenSession(valeur));
        } catch (RuntimeException invalide) {
            return Optional.empty();
        }
    }

    private Optional<Session> resoudreSession(TokenSession token) {
        TokenSessionHash hash = hasheurToken.hasher(token);
        return sessions.chargerParTokenHash(hash);
    }
}

package com.immo.gestion.session.test;

import com.immo.gestion.session.domain.TentativeDeConnexionEchouee;
import com.immo.gestion.session.domain.UtilisateurConnecte;
import com.immo.gestion.session.domain.UtilisateurDeconnecte;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Capture les events du BC session pour assertion en intégration.
 * Bean component-scanné par {@code @SpringBootTest}, hors classpath de prod.
 */
@Component
public class CapteurEventsSession {

    private final List<UtilisateurConnecte> connectes = new ArrayList<>();
    private final List<TentativeDeConnexionEchouee> echecs = new ArrayList<>();
    private final List<UtilisateurDeconnecte> deconnectes = new ArrayList<>();

    @EventListener
    void capter(UtilisateurConnecte e) { connectes.add(e); }

    @EventListener
    void capter(TentativeDeConnexionEchouee e) { echecs.add(e); }

    @EventListener
    void capter(UtilisateurDeconnecte e) { deconnectes.add(e); }

    public List<UtilisateurConnecte> connectes() { return connectes; }
    public List<TentativeDeConnexionEchouee> echecs() { return echecs; }
    public List<UtilisateurDeconnecte> deconnectes() { return deconnectes; }

    public void clear() {
        connectes.clear();
        echecs.clear();
        deconnectes.clear();
    }
}

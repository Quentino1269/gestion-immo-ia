# MISSION — Projet `gestion-immo-ia`

> Ce document est la **source de vérité** pour tous les agents (Claude Code et sous-agents) qui interviennent sur ce dépôt. Il prime sur toute habitude par défaut. À lire avant d'agir.

## 1. Mode opératoire en cours : **Mixte + Orchestrateur Multi-Agents** (depuis 2026-05-12)

Le projet est passé d'**Orchestration Métier stricte** à un **mode mixte** :

- **Implémentation autorisée** pour le périmètre validé (§2 ci-dessous). Les agents peuvent écrire et modifier du code applicatif (Java backend, React/TypeScript frontend, configs, migrations SQL, tests) **uniquement** sur les slices listés en §2.
- **Orchestration Métier maintenue** pour tout le reste du périmètre métier (§3). Pas de code applicatif sur ces sujets tant qu'ils n'ont pas été modélisés et validés selon la méthode habituelle.

Règles transverses :

- Avant d'écrire du code, vérifier que le slice concerné figure bien dans la liste §2.
- Si une demande utilisateur **dépasse** le périmètre §2 sans avoir été modélisée, **signaler** la contradiction et proposer soit (a) modéliser d'abord, soit (b) un stub minimal honnêtement borné.
- L'ubiquitous language des slices fait foi pour le nommage du code (classes, méthodes, tables, endpoints).
- En cas de divergence entre un slice validé et le code, **le slice prime** : on adapte le code (ou on modifie explicitement le slice si une découverte d'implémentation l'impose).

### 1.bis Protocole d'Orchestration Multi-Agents (depuis 2026-05-13)

Pour chaque slice à implémenter, l'agent change explicitement de **casquette** et produit ses livrables avant de passer à la suivante. Trois casquettes :

| Casquette         | Mission                                                                                                                    | Livrables                                                                                            |
|-------------------|----------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| **BACKEND**       | Implémenter le domaine (aggregates, value objects, invariants), les ports (in/out) et les adapters (web REST + persistance JPA). | Code Java compilable, modèle JPA, controller REST, configuration Spring nécessaire.                  |
| **QA**            | Rédiger les tests d'intégration qui valident le triplet `command → invariants → event` puis la projection vers le read model.    | Tests JUnit + Spring Boot Test (+ Testcontainers PostgreSQL si nécessaire), couvrant golden path + cas de refus. |
| **FRONTEND**      | Implémenter l'écran et la liaison vers le port d'entrée du backend.                                                        | Composants React + appels `fetch` / client API, styles Tailwind, gestion d'erreurs.                  |

Règles d'orchestration :

- Pour chaque slice : ordre fixe **BACKEND → QA → FRONTEND**. La casquette QA n'est lancée qu'après que le backend compile ; la casquette frontend qu'après que les tests d'intégration passent.
- Chaque message produit par l'agent indique explicitement la casquette en cours : « **[BACKEND]** … », « **[QA]** … », « **[FRONTEND]** … ».
- **Porte qualité obligatoire** après la casquette FRONTEND, avant de demander confirmation à l'utilisateur : lancer dans l'ordre les skills `code-review`, `security-review`, `simplify`, puis `verify` (exercice end-to-end du comportement) et `run` (démonstration manuelle dans l'app, golden path + un cas de refus). Corriger les findings bloquants avant de solliciter la validation utilisateur ; les findings non bloquants peuvent être signalés à l'utilisateur pour arbitrage.
- À l'issue d'un slice, attendre confirmation de l'utilisateur (« slice X validé en implémentation ») avant de passer au suivant.

## 2. Périmètre **prêt pour implémentation** (slices validés)

Quatre slices validés, à implémenter ensemble en cohérence :

| Slice                              | Statut          | Fichier                                                | Rôle dans la chaîne fonctionnelle                  |
|------------------------------------|-----------------|--------------------------------------------------------|----------------------------------------------------|
| **Création d'un Utilisateur**      | ✅ **COMPLETED** (2026-05-13) | `docs/slices/creation-utilisateur.md`    | Inscription auto-publique (email, mdp argon2id).   |
| **Authentification**               | ✅ **COMPLETED** (2026-05-19) | `docs/slices/authentification.md`        | Login (token opaque 24 h) + logout. Multi-session. |
| **Enrichissement du Profil**       | ✅ **COMPLETED** (2026-06-??) | `docs/slices/enrichissement-profil.md`            | Bascule `MINIMAL → COMPLET` pour préparer le bail. |
| **Création d'un Bien**             | ✅ **COMPLETED** (2026-07-01) | `docs/slices/creation-bien.md`                    | Ajout au portefeuille (maison, appart, chambre coloc). |

**Limitations consenties pour cette première implémentation** :

- **Mono-propriétaire** : pas d'habilitations multiples (co-propriétaires, administrateurs délégués) — ce slice est *différé*. À l'implémentation, on considère `proprietaireInitialId = unique ayant droit` du bien.
- **Pas de modification post-création** des biens et des profils — slices différés.
- **Pas de bail** — slice différé.
- Le code Java déjà committé pour `CreerBien` (`reference`, `prixEnCentimes`) **doit être intégralement remplacé** par l'implémentation du slice validé. Pas d'effort de rétro-compatibilité.

## 3. Périmètre **encore en Orchestration Métier**

Aucun code applicatif sur ces sujets tant qu'ils n'ont pas été modélisés selon la méthode standard (§4) et validés explicitement par l'utilisateur :

- **Habilitations sur un Bien** (co-propriétaires, administrateurs délégués) — référencé en D13 du slice création de bien.
- **Modification du profil** (changements post-saisie : déménagement, etc.) — D6 du slice enrichissement-profil.
- **Signature d'un Bail** — consommateur principal de `statutProfil = COMPLET`.
- Récupération / changement de mot de passe, MFA, vérification d'email, sécurité du compte.
- Personne morale (SCI, SARL).
- Quittancement, charges, indexation IRL.
- État des lieux, travaux & incidents, reversement propriétaire, fin de bail.
- RGPD : suppression de compte, purge automatique du `JournalConnexions`.
- Bounded context « Documents » (pièces d'identité, scans).

L'ordre exact des prochains slices à modéliser sera décidé selon les besoins ; la mémoire projet (`memory/project_slices_status.md`) tient le compte.

## 4. Méthode (inchangée)

On suit **Event Modeling** (Adam Dymitruk), avec le format établi par les slices validés :

- **Command** (bleu) — intention utilisateur
- **Event** (orange) — fait métier immuable, au passé
- **Read Model** (vert) — projection
- **UI** (gris) — interface
- **Aggregate** (violet) — frontière de cohérence

Pour chaque nouveau slice : narration courte → diagramme Mermaid → tableaux des champs (commande / event / read model) → règles métier → questions ouvertes → décisions actées (§1 du slice) une fois validé.

## 5. Stack figée (rappel, ne pas rediscuter sans demande explicite)

- Backend : **Spring Boot 3.4 / Java 21 / Maven** (Maven imposé — contrainte CPU Intel).
- Frontend : **React 18 + Vite + TypeScript + Tailwind CSS v4** (intégration native via `@tailwindcss/vite`, pas de PostCSS séparé).
- BDD : **PostgreSQL 16** (Docker, `infrastructure/docker-compose.yml`).
- Architecture : **hexagonale** (ports & adapters), un **package racine par bounded context** : `com.immo.gestion.utilisateur`, `…session`, `…bien`, `…shared`. Chaque bounded context contient ses sous-paquets `domain` (model + port), `application` (use cases), `adapter` (in.web + out.persistence).
- Auth : **token opaque** (≥ 128 bits, hash sha-256 stocké). Pas de JWT. Filter Spring **custom** lisant `Authorization: Bearer <token>` (pas de Spring Security en V1).
- Hash mdp : **argon2id** via `de.mkammerer:argon2-jvm` (paramètres OWASP 2024).
- Events : **Event Sourcing strict** — la table `event_store` (JSONB, append-only, contrainte unique `(stream_id, version)`) est la source de vérité pour les aggregates `Utilisateur`, `Session`, `Bien` ; l'état courant de chaque aggregate est reconstruit à la volée par rejeu (`X.reconstruire(...)`) à chaque chargement, sans snapshot en V1. Écriture : `EventStore.append(streamId, streamType, expectedVersion, evenements)` dans la même transaction que la commande (contrôle de concurrence optimiste via la contrainte unique, `ConflitDeVersionException` en cas de conflit). Projections : `ApplicationEventPublisher` Spring + listeners `@EventListener` synchrones, déclenchés juste après l'append, dans la même transaction, qui maintiennent à jour les tables JPA existantes (`utilisateurs`, `sessions`, `biens`) désormais traitées comme de purs read models — plus jamais écrites directement par le domaine. Les lectures qui n'engagent pas d'écriture (recherche par email, par token, portefeuille, fiche bien) passent par des ports `XQueryRepository` dédiés adossés à ces mêmes tables ; seul le chargement de l'aggregate qu'on s'apprête à modifier passe par le rejeu. `TentativeDeConnexionEchouee` (fait d'audit sans aggregate) n'entre pas dans `event_store` : persistée dans une table dédiée `tentatives_connexion_echouees`. Pas d'upcasting d'événements ni d'outbox en V1 (volume d'events par aggregate faible).
- Migrations DB : `spring.jpa.hibernate.ddl-auto=update` en V1 (Flyway introduit plus tard si besoin — y compris pour `event_store`).

## 6. Définitions de « fini »

### 6.1. Pour un **slice métier** (mode Orchestration Métier)

1. Document `docs/slices/<nom-du-slice>.md` existe et contient :
   - Décisions actées (§1)
   - Contexte et dépendances
   - Diagramme Mermaid
   - Commandes, events, read models avec tableaux des champs
   - Invariants numérotés
   - Aucune question résiduelle non tranchée
2. Ubiquitous language à jour (glossaire en français au sein du slice).
3. Validation explicite de l'utilisateur (« slice prêt »).

### 6.2. Pour une **fonctionnalité implémentée** (mode Implémentation)

1. Code conforme au slice de référence (commandes, events, invariants couverts).
2. Tests automatisés couvrant :
   - Les invariants (chemins nominaux et de refus).
   - Le contrat d'API (REST controllers / DTO).
   - La projection des read models à partir des events.
3. Documentation API exposée (OpenAPI ou équivalent généré).
4. Aucune erreur sur la chaîne `mvn verify` (backend) et `npm run build` + `npm test` (frontend).
5. Démonstration manuelle réussie de bout en bout (golden path + au moins un cas de refus).
6. Porte qualité passée (voir §1.bis) : skills `code-review`, `security-review`, `simplify`, `verify`, `run` exécutés sur le diff du slice, findings bloquants corrigés.

## 7. Communication

- Langue de travail : **français**.
- Réponses concises, orientées action.
- Si un agent identifie une contradiction entre ce document et une instruction utilisateur ponctuelle, il la signale avant de procéder.
- Toute découverte d'implémentation qui contredit un slice validé doit être remontée à l'utilisateur **avant** modification du slice ou du code, sauf bug évident.

---

**Historique** :
- 2026-05-12 — Création initiale (mode Orchestration Métier strict).
- 2026-05-12 — Bascule en mode mixte après validation des 4 premiers slices.
- 2026-05-13 — Ajout du protocole Orchestrateur Multi-Agents (BACKEND → QA → FRONTEND par slice). Stack précisée : Tailwind v4, custom filter auth, argon2-jvm, packages par bounded context, projections via `ApplicationEventPublisher`.
- 2026-05-13 — Slice **Création d'un Utilisateur** implémenté end-to-end (commit `d24367b`).
- 2026-05-19 — Slice **Authentification** implémenté end-to-end (aggregate `Session`, token opaque CSPRNG 256 bits + SHA-256, dummy hash anti-énumération, `BearerAuthFilter`, frontend `LoginPage`/`AccueilPage` + `AuthProvider`). 35/35 tests unitaires verts ; tests d'intégration Testcontainers `@Disabled` faute de compat Docker Desktop 29 / `docker-java` sur Mac Intel.
- 2026-06-?? — Slice **Enrichissement du Profil** implémenté end-to-end (commit `5f50d89`). Aggregate `Utilisateur` étendu, `StatutProfil MINIMAL→COMPLET`, 5 events, read model `ProfilUtilisateur`, 22 tests verts.
- 2026-07-01 — Slice **Création d'un Bien** implémenté end-to-end. Bounded context `bien` créé (aggregate, 3 types, invariants I-1..I-8 + COLOC + CHARGES), `Adresse` migré vers `shared`, 30 tests verts, frontend portefeuille + formulaire nouveau bien.
- 2026-07-15 — Ajout d'une porte qualité obligatoire en fin de protocole (§1.bis, §6.2) : skills `code-review`, `security-review`, `simplify`, `verify`, `run` à exécuter systématiquement après la casquette FRONTEND, avant validation utilisateur.
- 2026-07-16 — Migration vers l'Event Sourcing strict sur les 3 bounded contexts existants (`utilisateur`, `session`, `bien`) : table `event_store` (JSONB append-only, OCC via contrainte unique), aggregates reconstruits par rejeu (`reconstruire(...)`), ports repository scindés écriture (event-sourcée) / lecture (`XQueryRepository` adossé aux projections `utilisateurs`/`sessions`/`biens`), `TentativeDeConnexionEchouee` journalisée dans `tentatives_connexion_echouees`. Correctif de fond : `UtilisateurConnecte` gagne le champ `tokenHash` (nécessaire au rejeu de `Session`). Aucun changement de contrat REST. Voir `docs/slices/*.md` (inchangés, modèle événementiel identique) et le plan `.claude/plans/effervescent-meandering-pnueli.md`.

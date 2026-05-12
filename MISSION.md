# MISSION — Projet `gestion-immo-ia`

> Ce document est la **source de vérité** pour tous les agents (Claude Code et sous-agents) qui interviennent sur ce dépôt. Il prime sur toute habitude par défaut. À lire avant d'agir.

## 1. Mode opératoire en cours : **Mixte** (depuis 2026-05-12)

Le projet est passé d'**Orchestration Métier stricte** à un **mode mixte** :

- **Implémentation autorisée** pour le périmètre validé (§2 ci-dessous). Les agents peuvent écrire et modifier du code applicatif (Java backend, React/TypeScript frontend, configs, migrations SQL, tests) **uniquement** sur les slices listés en §2.
- **Orchestration Métier maintenue** pour tout le reste du périmètre métier (§3). Pas de code applicatif sur ces sujets tant qu'ils n'ont pas été modélisés et validés selon la méthode habituelle.

Règles transverses :

- Avant d'écrire du code, vérifier que le slice concerné figure bien dans la liste §2.
- Si une demande utilisateur **dépasse** le périmètre §2 sans avoir été modélisée, **signaler** la contradiction et proposer soit (a) modéliser d'abord, soit (b) un stub minimal honnêtement borné.
- L'ubiquitous language des slices fait foi pour le nommage du code (classes, méthodes, tables, endpoints).
- En cas de divergence entre un slice validé et le code, **le slice prime** : on adapte le code (ou on modifie explicitement le slice si une découverte d'implémentation l'impose).

## 2. Périmètre **prêt pour implémentation** (slices validés)

Quatre slices validés, à implémenter ensemble en cohérence :

| Slice                              | Fichier                                                | Rôle dans la chaîne fonctionnelle                  |
|------------------------------------|--------------------------------------------------------|----------------------------------------------------|
| **Création d'un Utilisateur**      | `docs/slices/creation-utilisateur.md`                  | Inscription auto-publique (email, mdp argon2id).   |
| **Authentification**               | `docs/slices/authentification.md`                      | Login (token opaque 24 h) + logout. Multi-session. |
| **Enrichissement du Profil**       | `docs/slices/enrichissement-profil.md`                 | Bascule `MINIMAL → COMPLET` pour préparer le bail. |
| **Création d'un Bien**             | `docs/slices/creation-bien.md`                         | Ajout au portefeuille (maison, appart, chambre coloc). |

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
- Frontend : **React 18 + Vite + TypeScript**.
- BDD : **PostgreSQL 16** (Docker, `infrastructure/docker-compose.yml`).
- Architecture : **hexagonale** (ports & adapters). Découpage en modules / bounded contexts à affiner selon les slices.
- Auth : **token opaque** (≥ 128 bits, hash sha-256 stocké). Pas de JWT.
- Hash mdp : **argon2id** (paramètres OWASP 2024).

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

## 7. Communication

- Langue de travail : **français**.
- Réponses concises, orientées action.
- Si un agent identifie une contradiction entre ce document et une instruction utilisateur ponctuelle, il la signale avant de procéder.
- Toute découverte d'implémentation qui contredit un slice validé doit être remontée à l'utilisateur **avant** modification du slice ou du code, sauf bug évident.

---

**Historique** :
- 2026-05-12 — Création initiale (mode Orchestration Métier strict).
- 2026-05-12 — Bascule en mode mixte après validation des 4 premiers slices.

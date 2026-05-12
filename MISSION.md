# MISSION — Projet `gestion-immo-ia`

> Ce document est la **source de vérité** pour tous les agents (Claude Code et sous-agents) qui interviennent sur ce dépôt. Il prime sur toute habitude par défaut. À lire avant d'agir.

## 1. Mode opératoire en cours : **Orchestration Métier**

Nous ne sommes **pas** en mode "écrire du code". Nous sommes en mode **modélisation du domaine**. Toute production doit servir la compréhension métier avant la production technique.

Règles pour les agents :

- **Ne pas écrire / modifier de code applicatif** (Java, TypeScript, SQL, configs Spring, etc.) tant que le modèle métier du périmètre courant n'est pas validé par l'utilisateur.
- Les artefacts produits sont, par ordre de priorité :
  1. Documents d'**Event Modeling** (sous `docs/`) — slices, commandes, événements, read models, règles métier.
  2. **Ubiquitous language** — glossaire des termes du métier (français).
  3. Diagrammes (Mermaid) intégrés aux documents.
  4. Questions ouvertes à poser à l'utilisateur pour lever les ambiguïtés.
- Le code déjà commité (slice "Création d'un bien") **reste figé** : on ne le refactore pas tant que le modèle global n'est pas posé.
- En cas de doute sur le périmètre d'une demande : **demander avant de produire**.

## 2. Priorité actuelle

> **Modélisation métier de la « Gestion locative d'un portefeuille de biens immobiliers »**

C'est le périmètre exclusif des prochains travaux. Aucun travail technique (implémentation, refactor, migration, infra) ne doit être engagé tant que ce modèle n'est pas posé et validé.

### Périmètre métier à explorer (non exhaustif, à raffiner avec l'utilisateur)

- Cycle de vie d'un **bien** dans le portefeuille (création, mise en location, vacance, sortie).
- **Locataire** et **mandat de gestion** (qui mandate l'agence ? sur quels biens ?).
- **Bail** : signature, état des lieux entrée/sortie, dépôt de garantie, durée, renouvellement, résiliation.
- **Quittancement** : appel de loyer, paiement, régularisation des charges, retards.
- **Travaux & incidents** : déclaration, devis, intervention, refacturation.
- **Reversement propriétaire** : honoraires, comptes rendus de gestion.
- **Fin de bail** : préavis, état des lieux de sortie, restitution du dépôt.

L'objectif est de produire, pour chaque slice retenu, un document `docs/event-modeling-<slice>.md` au même format que `docs/event-modeling.md` existant.

## 3. Méthode

On suit **Event Modeling** (Adam Dymitruk), cohérent avec l'existant :

- **Command** (bleu) — intention utilisateur
- **Event** (orange) — fait métier immuable
- **Read Model** (vert) — projection
- **UI** (gris) — interface
- **Aggregate** (violet) — frontière de cohérence

Pour chaque slice : narration courte → diagramme Mermaid → tableaux des champs (commande / event / read model) → règles métier → questions ouvertes.

## 4. Stack figée (rappel, ne pas rediscuter sans demande explicite)

- Backend : **Spring Boot 3.4 / Java 21 / Maven** (Maven imposé — contrainte CPU Intel).
- Frontend : **React 18 + Vite + TypeScript**.
- BDD : **PostgreSQL 16** (Docker).
- Architecture : **hexagonale** (ports & adapters), un module par bounded context une fois le découpage validé.

## 5. Définition de "fini" pour le mode actuel

Un slice métier est considéré comme prêt à passer à l'implémentation quand :

1. Le document `docs/event-modeling-<slice>.md` existe et contient diagramme + tableaux + règles + questions résolues.
2. L'**ubiquitous language** est à jour (glossaire en français).
3. L'utilisateur a explicitement validé : "ce slice est prêt à coder".

Tant que ces trois conditions ne sont pas remplies sur un slice, **aucune ligne de code applicatif** n'est produite pour ce slice.

## 6. Communication

- Langue de travail : **français**.
- Réponses concises, orientées action métier.
- Si un agent identifie une contradiction entre ce document et une instruction utilisateur ponctuelle, il la signale avant de procéder.

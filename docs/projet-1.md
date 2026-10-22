# Projet 1 : Monolithe Spring Boot (détail)

> **Note d'usage.** Ce fichier détaille le projet 1 de MiaPay-lite (voir `MiaPay-lite.md` pour le plan global et `stack-technique.md` pour les outils). Il sert de référence pour le modèle de données, les endpoints, les règles de gestion, la stratégie de concurrence et le backlog. Tout est modifiable : quand une décision change, on met ce fichier à jour.

---

## 1. Objectif

Maîtriser Spring Boot en profondeur avant de le distribuer. Un marchand génère un QR pour un montant donné, un acheteur le paie avec un OTP, et les deux voient leur historique. L'opérateur est un module interne.

**Défi central :** 50 confirmations simultanées sur un même QR ne produisent qu'un seul paiement, et un rejeu avec la même clé d'idempotence ne change rien.

**Critère de réussite :** le test de concurrence passe de façon fiable, répété 100 fois.

---

## 2. Décisions techniques

| Sujet | Décision |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | Version stable la plus récente de la branche 4.x (jamais SNAPSHOT, milestone ou RC) |
| Build | Maven, packaging jar |
| springdoc | Série 3.x (compatible Spring Boot 4), dépendance `springdoc-openapi-starter-webmvc-ui` |
| Base de données | PostgreSQL 17 (version fixée dans Compose), migrations Flyway |
| Hibernate | `ddl-auto: validate` (Flyway est seul maître du schéma), `open-in-view: false` |
| Structure | Monolithe modulaire, un package par fonctionnalité : `auth`, `operator`, `qr`, `payment`, `common` |
| Opérateur | Module interne appelé **uniquement via une interface** (`OperatorPort`), pour pouvoir l'extraire au projet 3 |
| Environnement | GitHub Codespaces (VS Code), Docker Compose pour PostgreSQL |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL réel) |
| Authentification | JWT signé en **HS256** avec un secret partagé (`JWT_SECRET`, 32 octets minimum), via `oauth2-resource-server` de Spring Security. Remplacé par Keycloak au projet 5 |
| Mots de passe | **BCrypt** via `DelegatingPasswordEncoder` (hash préfixé `{bcrypt}`, 72 caractères maximum) |
| Erreurs d'API | Corps `ProblemDetail` (RFC 9457) : 400 pour la validation, 409 pour un conflit métier |

---

## 3. Modèle de données

Tous les montants sont des entiers en FCFA (`BIGINT`), jamais des flottants. Horodatages en UTC (`TIMESTAMPTZ`).

### `users`
- `id` UUID (clé primaire)
- `email` unique
- `password_hash`
- `role` : `BUYER` ou `MERCHANT` (contrainte `CHECK`)
- `created_at`

### `operator_accounts`
- `id` UUID, `user_id` UUID (unique, clé étrangère vers `users`)
- `balance` BIGINT, avec `CHECK (balance >= 0)` : dernier filet contre un solde négatif
- `version` (verrou optimiste)

Créé automatiquement à l'inscription, dans la même transaction que l'utilisateur : solde de départ de 100 000 FCFA pour l'acheteur, 0 pour le marchand (valeur portée par l'enum `Role`).

Côté code, `OperatorAccount` ne référence que l'identifiant `userId` (pas l'objet `User`), pour que le module `operator` ne dépende pas du module `auth`.

### `account_movements`
Grand livre : chaque mouvement d'argent est tracé.
- `id`, `account_id`, `payment_id`
- `type` : `DEBIT` ou `CREDIT`
- `amount`, `created_at`

### `qr_codes`
- `id` UUID (c'est le contenu du QR)
- `merchant_id`
- `amount`
- `status` : `ACTIVE`, `RESERVED`, `USED`, `EXPIRED`, `REVOKED`
- `expires_at`
- `version`

### `payments`
- `id`, `qr_id`, `buyer_id`, `merchant_id`
- `amount`
- `status` : `CREATED`, `PENDING_OTP`, `DEBITED`, `EXPIRED`, `FAILED`, `CANCELLED`
- `otp_hash`, `otp_expires_at`, `otp_attempts`
- `version`, `created_at`, `updated_at`

### `idempotency_keys`
- `user_id`, `key`
- `request_hash`, `response_status`, `response_body`
- `created_at`
- Contrainte **unique (user_id, key)**

### `payment_transitions`
Mini-journal des changements d'état (deviendra l'audit au projet 5).
- `payment_id`, `from_status`, `to_status`, `at`

> Dans ce projet, la machine d'états s'arrête à `DEBITED`. Le reversement au marchand (`REVERSÉ`) et le remboursement arrivent au projet 3 et suivants.

---

## 4. Endpoints

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/auth/register` | Inscription (choix du rôle) |
| POST | `/auth/login` | Connexion, renvoie un JWT |
| GET | `/me` | Profil et solde |
| POST | `/qr` | Marchand : crée un QR `{amount}` |
| GET | `/qr` | Marchand : liste de ses QR |
| GET | `/qr/{id}` | Détail et statut d'un QR |
| POST | `/qr/{id}/revoke` | Marchand : révoque un QR |
| POST | `/payments` | Acheteur : initie `{qrId}`, réserve le QR, génère l'OTP (en-tête `Idempotency-Key`) |
| POST | `/payments/{id}/confirm` | Acheteur : confirme `{otp}` (en-tête `Idempotency-Key`) |
| POST | `/payments/{id}/cancel` | Acheteur : annule |
| GET | `/payments` | Historique (filtré selon le rôle) |
| GET | `/payments/{id}` | Détail d'un paiement |
| GET | `/operator/inbox` | Simule le SMS de l'opérateur pour récupérer l'OTP sans vrai SMS |

---

## 5. Règles de gestion

- Montant entier strictement positif, avec un plafond (par exemple 500 000 FCFA).
- QR valable 5 minutes, usage unique, révocable par le marchand.
- OTP à 6 chiffres, valable 2 minutes, stocké haché, 3 essais maximum.
- Un acheteur ne peut pas payer son propre QR.
- Un QR ne peut avoir qu'un seul paiement actif à la fois.
- Si l'OTP expire, est annulé ou épuisé : la réservation du QR est libérée, ou le QR passe en `EXPIRED` s'il est trop vieux.
- Solde insuffisant : le paiement passe en `FAILED`.
- Toute transition passe par la machine d'états : une transition illégale est refusée, une transition déjà faite est rejouée sans effet.
- Clé d'idempotence obligatoire sur l'initiation et la confirmation.

---

## 6. Concurrence : le cœur du projet

Trois protections empilées, à tester séparément.

1. **Réservation du QR.** Mise à jour conditionnelle atomique :
   `UPDATE qr_codes SET status = 'RESERVED' WHERE id = ? AND status = 'ACTIVE' AND expires_at > now()`.
   Une seule ligne affectée désigne le gagnant.
2. **Débit.** Soit `UPDATE ... SET balance = balance - ? WHERE balance >= ?`, soit verrou pessimiste (`SELECT ... FOR UPDATE`) sur le compte.
3. **Idempotence.** La contrainte unique sur `(user_id, key)` garantit qu'une seule requête exécute l'opération ; les rejeux reçoivent la réponse stockée.

**Exercice d'apprentissage :** implémenter la confirmation avec le verrou **optimiste** (`@Version`), puis avec le verrou **pessimiste**, mesurer les deux dans le test de concurrence, et consigner la comparaison dans un ADR (`docs/adr/`).

### Scénarios de test

Tous avec Testcontainers, `ExecutorService` et `CountDownLatch`, répétés 100 fois (`@RepeatedTest(100)`).

| Scénario | Situation | Résultat attendu |
|---|---|---|
| A | 50 acheteurs initient un paiement sur le même QR | 1 seule réservation, 49 refus (409) |
| B | 50 confirmations du même paiement avec des clés différentes | 1 seul débit, solde débité une seule fois |
| C | 50 requêtes avec la même clé d'idempotence | 1 exécution, 50 réponses identiques |

---

## 7. Front (minimaliste)

PWA React + TypeScript de 4 écrans, composants standards, aucun design sur mesure :
1. Inscription / connexion (choix acheteur ou marchand)
2. Marchand : encaisser (montant, QR généré, statut, ventes)
3. Acheteur : payer (contenu du QR, montant et marchand, saisie de l'OTP)
4. Historique (commun aux deux rôles)

Client d'API généré depuis l'OpenAPI de Spring (`openapi-typescript`). Le front est fait en dernier : tout le backend se teste d'abord via Swagger UI.

---

## 8. Backlog par jalons

### M0 : Socle
- [x] Dépôt GitHub, README, `.gitignore`, licence MIT
- [x] Devcontainer (Codespaces, Java 21, Docker, extensions)
- [x] Projet généré avec Spring Initializr (Maven, jar)
- [x] `docker-compose.yml` avec PostgreSQL (version fixée, volume, healthcheck), identifiants dans `.env`
- [x] `application.yml` (datasource, `ddl-auto: validate`, Actuator)
- [x] Flyway `V1__init.sql`
- [x] springdoc, Swagger UI
- [x] Testcontainers qui démarre
- **Critère :** `/actuator/health` est `UP`, Swagger UI s'affiche, `./mvnw verify` passe.

### M1 : Authentification
- [x] Migration pour les comptes opérateur (`V2__operator_accounts.sql`)
- [x] Inscription (email unique, mot de passe haché, rôle)
- [ ] Connexion et émission de JWT
- [ ] Spring Security (resource server, règles par rôle)
- [x] Création automatique du compte opérateur à l'inscription
- [ ] Tests d'intégration de l'auth (le service d'inscription est testé, y compris la course sur l'email ; restent les tests HTTP)

### M2 : Module opérateur
- [ ] Interface `OperatorPort` et implémentation interne
- [ ] Comptes, soldes, mouvements
- [ ] Génération et vérification de l'OTP (haché, expiration, essais)
- [ ] Endpoint `/operator/inbox`

### M3 : QR
- [ ] Création d'un QR (montant, expiration)
- [ ] Consultation, liste, révocation
- [ ] Génération de l'image du QR (ZXing, à confirmer pour ce projet ou le projet 2)

### M4 : Paiement
- [ ] Machine d'états stricte
- [ ] Initiation (réservation atomique du QR, génération de l'OTP)
- [ ] Confirmation (vérification OTP, débit)
- [ ] Annulation, historique
- [ ] Journal `payment_transitions`

### M5 : Idempotence
- [ ] En-tête `Idempotency-Key` obligatoire
- [ ] Table `idempotency_keys`, stockage et rejeu des réponses
- [ ] Gestion du cas de deux requêtes simultanées avec la même clé

### M6 : Expirations
- [ ] Tâche `@Scheduled` pour l'expiration des QR
- [ ] Tâche `@Scheduled` pour l'expiration des OTP / paiements en attente
- [ ] Libération des réservations

### M7 : Tests de concurrence
- [ ] Scénario A, B, C
- [ ] Comparaison verrou optimiste / pessimiste (ADR)
- [ ] Critère : 100 répétitions sans échec

### M8 : Docker
- [ ] Dockerfile multi-stage
- [ ] Docker Compose complet (base + application)

### M9 : Front
- [ ] Les 4 écrans React
- [ ] Client d'API généré depuis l'OpenAPI

### M10 : Livrables
- [ ] README en anglais
- [ ] Schéma d'architecture
- [ ] Court post LinkedIn

---

## 9. Conventions rappelées

- Montants entiers (`long`), jamais de flottants.
- Horodatages en UTC.
- Clé d'idempotence obligatoire sur toute opération de paiement.
- Une migration Flyway appliquée ne se modifie jamais : toute évolution est une nouvelle migration `V<n>__description.sql`.
- Pas de secrets dans Git (`.env` ignoré, `.env.example` commité).
- README, code et commits en anglais ; échanges de travail en français.

---

## 10. Sujets à approfondir au fil du projet

- Choix final verrou optimiste vs pessimiste (ADR)
- Stratégie de hachage de l'OTP (le mot de passe est tranché : BCrypt)
- Format des erreurs d'API : `ProblemDetail` retenu pour la validation (400) et l'email déjà utilisé (409) ; à compléter pour les autres cas (401, 403, 404, paiements)
- Stratégie de tests : proportion unitaires / intégration

# MiaPay-lite : plan des 7 projets

> **Note d'usage.** Ce fichier est la référence du projet fil rouge « MiaPay-lite ». Il décrit l'application et les 7 projets à réaliser l'un après l'autre. À chaque étape, on s'appuie sur la section du projet concerné pour l'approfondir : modèle de données, endpoints, règles de gestion, tests, backlog de tickets, configuration. Le fichier `stack-technique.md` liste et explique les outils et technologies utilisés.

---

## 1. Présentation du projet

MiaPay-lite est une plateforme de paiement par QR code, conçue comme fil rouge pour pratiquer **Spring Boot**, l'**architecture microservices** et le **déploiement sur AWS** (en restant dans le free tier).

Le parcours fonctionnel :

1. Le marchand génère un QR code pour un montant donné.
2. L'acheteur le scanne et confirme le paiement avec un code **OTP** de son opérateur.
3. Son compte est débité.
4. Les fonds sont reversés au marchand.
5. Les deux parties sont notifiées et voient la transaction évoluer.

### Principes

- **Un seul opérateur simulé** (`operator-sim`), où acheteurs et marchands ont leur compte. Aucun système tiers réel : tout est codé dans le projet, y compris des pannes et lenteurs volontaires pour provoquer de vrais cas d'échec.
- **MiaPay ne détient aucun fonds** : c'est un prestataire technique qui orchestre les flux. Il parle à l'opérateur via une interface (port/adapter), ce qui garde le système extensible.
- **Un seul type de QR code** : le QR dynamique.
- **Tout est automatique** : aucun staff, aucune validation ni intervention manuelle. Les décisions (niveaux de confiance, suspension, remboursement, expiration) sont prises par des règles.
- **Montants entiers en FCFA** (pas de décimales, donc pas d'erreurs d'arrondi) et argent entièrement fictif.
- **Front minimaliste** : une petite PWA React de 4 écrans, environ 10 % de chaque projet, avec des composants standards. Le but est d'apprendre Spring Boot, les microservices et AWS, pas le développement front.

---

## 2. Le QR code dynamique

Le QR est créé par le marchand pour un montant fixé, expire en quelques minutes et ne peut être utilisé qu'une seule fois.

- **Usage unique** : c'est le défi de concurrence central du parcours. Deux acheteurs qui confirment le même QR en même temps, un seul doit payer.
- **Expiration** : elle justifie les tâches planifiées, l'état `EXPIRÉ` et la réconciliation.
- **Montant fixé côté marchand** : le front de l'acheteur est plus simple (aucun montant à saisir) et la validation métier est plus nette.

---

## 3. Règles automatiques

- **Inscription** : le compte est créé immédiatement, avec un compte opérateur fictif et un solde de départ pour l'acheteur. Le rôle (acheteur ou marchand) est choisi à l'inscription.
- **Niveaux de confiance du marchand** (plafonds associés) : niveau 1 à l'inscription, passage automatique au niveau supérieur après un volume de paiements réussis, rétrogradation automatique si le taux d'échec devient anormal.
- **Suspension automatique** par le service de conformité (trop de paiements suspects en peu de temps), levée automatique après un délai.
- **Remboursement et rejeu** : si le reversement échoue, il est rejoué plusieurs fois, puis l'acheteur est remboursé automatiquement.
- **Expiration** : un QR dynamique et un paiement en attente d'OTP expirent seuls.

---

## 4. Machine d'états d'un paiement

```
CRÉÉ → EN ATTENTE OTP → DÉBITÉ → REVERSÉ
```

Sorties possibles : `EXPIRÉ`, `ÉCHOUÉ`, `ANNULÉ`, `REMBOURSÉ`.

Règle : toute transition est validée par la machine d'états (aucun état ne peut être sauté ou atteint deux fois), et chaque transition est idempotente.

---

## 5. Architecture cible

Elle se construit progressivement au fil des projets.

| Composant | Rôle | Apparaît au projet |
|---|---|---|
| Monolithe Spring Boot | Toute l'application (opérateur en module interne) | 1 |
| `merchant-service` | Marchands, QR, niveaux de confiance | 3 |
| `payment-service` | Orchestration du paiement, machine d'états, reversement | 3 |
| `operator-sim` | Opérateur simulé : comptes, soldes, OTP, pannes et lenteurs aléatoires | 3 |
| API Gateway | Point d'entrée unique | 3 |
| `notification-service` | E-mails et événements à chaque étape | 4 |
| `compliance-service` | Règles de fréquence et de montant, suspension automatique | 4 |
| Broker de messages | Kafka ou RabbitMQ (local), SQS/SNS (AWS) | 4 |
| Fournisseur d'identité | Keycloak (ou Cognito) | 5 |
| Redis | Cache et limitation de débit distribuée | 6 |
| Fonctions Lambda, Step Functions, DynamoDB | Traitements planifiés, rapports, audit | 7 |

---

## 6. Front (toute l'application)

| Écran | Rôle |
|---|---|
| Inscription / connexion | Choix acheteur ou marchand |
| Marchand : encaisser | Saisir un montant, afficher le QR généré, voir son statut et ses ventes |
| Acheteur : payer | Coller ou saisir le contenu du QR, voir montant et marchand, saisir l'OTP |
| Historique | Transactions avec statut (commun aux deux rôles) |

Règles pour le front : composants standards prêts à l'emploi, aucun design sur mesure, un seul fichier de client API généré depuis l'OpenAPI de Spring.

---

## 7. Conventions de travail (proposées, modifiables)

- Montants stockés en **entiers** (FCFA, type `long`), jamais en flottants.
- **Clé d'idempotence** obligatoire sur toute opération de paiement (en-tête HTTP dédié).
- Horodatages en **UTC**.
- Une base de données **par service** à partir du projet 3.
- README de chaque projet en **anglais** (pour GitHub et les recruteurs).
- Chaque projet se termine par : un critère de réussite vérifié, un README, un schéma d'architecture, un court post LinkedIn.

---

## 8. Les 7 projets

### Projet 1 : Monolithe Spring Boot

**Objectif d'apprentissage :** maîtriser Spring Boot en profondeur avant de le distribuer.

**Ce que fait l'application :** un marchand génère un QR pour un montant donné, un acheteur le paie avec un OTP, et les deux voient l'historique. L'opérateur est un module interne.

**Fonctionnalités :**
- Inscription libre, connexion JWT, compte opérateur fictif créé automatiquement (solde de départ pour l'acheteur)
- Création d'un QR dynamique : montant fixé, expiration courte, usage unique, révocation possible
- Paiement en deux temps : initiation (le QR est alors réservé), puis confirmation par OTP
- Débit de l'acheteur, historique pour les deux parties
- Expiration automatique du QR et de l'OTP

**Conteneurisation :** Docker Compose pour lancer PostgreSQL en local, Dockerfile multi-stage pour l'application, Testcontainers pour les tests d'intégration (il démarre un vrai PostgreSQL, donc Docker est requis).

**Front :** les 4 écrans décrits plus haut, sans style particulier.

**Défi technique :** 50 confirmations simultanées sur un même QR ne produisent qu'un paiement, et un rejeu avec la même clé d'idempotence ne change rien (verrou optimiste vs pessimiste, machine d'états stricte).

**Critère de réussite :** ce test de concurrence passe de façon fiable, répété 100 fois.

**À approfondir le moment venu :** modèle de données, endpoints, règles de gestion, stratégie de tests de concurrence, backlog de tickets.

---

### Projet 2 : Déploiement AWS

**Objectif d'apprentissage :** transformer les connaissances SAA en pratique, avec Terraform et CI/CD.

**Nouvelles fonctionnalités :**
- Image du QR code générée et stockée sur S3 (URL pré-signée)
- E-mail de confirmation aux deux parties
- Reçu PDF par transaction sur S3
- Environnements dev / « prod » en Terraform, CI/CD

**Conteneurisation :** image de l'application construite dans la CI (GitHub Actions), poussée sur ECR, lancée sur EC2 avec Docker.

**Front :** affichage de l'image du QR et lien de reçu. Build déployé sur S3 + CloudFront.

**Critère de réussite :** un push met à jour back et front, et l'environnement complet se recrée depuis zéro en moins de 15 minutes.

**Coût :** rester dans le free tier, budget d'alerte actif, `terraform destroy` en fin de session.

**À approfondir le moment venu :** schéma d'architecture AWS, modules Terraform, pipeline GitHub Actions, IAM à moindre privilège.

---

### Projet 3 : Découpage en microservices

**Objectif d'apprentissage :** comprendre pourquoi et comment distribuer, y compris les inconvénients.

**Ce que fait l'application :** l'opérateur devient un **vrai système distinct** qui peut tomber, et le **reversement** au marchand apparaît comme étape séparée.

**Services :** `merchant-service` (marchands, QR, niveaux de confiance), `payment-service` (orchestration), `operator-sim` (comptes, OTP, pannes et lenteurs aléatoires), API Gateway.

**Nouvelles fonctionnalités :**
- Reversement au marchand après débit de l'acheteur
- Plafonds par niveau de confiance (par transaction, par jour), niveau 1 attribué automatiquement
- Résilience : timeout, retry et circuit breaker (Resilience4j) sur les appels à l'opérateur
- Mode dégradé : si l'opérateur tombe, les paiements échouent proprement, mais les marchands peuvent toujours créer des QR et consulter leur historique

**Conteneurisation :** Docker Compose pour lancer tout le système en local (les 3 services, la gateway et une base de données par service).

**Front :** messages d'erreur plus précis (opérateur indisponible, plafond dépassé).

**Critère de réussite :** coupure d'`operator-sim` en plein paiement : aucun débit n'est perdu ou orphelin, et le reste de l'application répond.

**À approfondir le moment venu :** découpage des responsabilités, contrats d'API entre services, configuration Resilience4j, tests de contrat.

---

### Projet 4 : Architecture événementielle

**Objectif d'apprentissage :** la compétence la plus valorisée en entretien senior (saga, cohérence éventuelle, idempotence).

**Nouvelles fonctionnalités :**
- **Saga de paiement** : débit → `PaymentDebited` → reversement, avec remboursement automatique de l'acheteur si le reversement échoue
- **Reversement avec retry** automatique, puis remboursement après épuisement des tentatives
- **Reversement différé** (immédiat ou regroupé en lot)
- **`notification-service`** : e-mails et événements à chaque étape
- **Réconciliation automatique** des paiements restés en attente
- **Évolution automatique du niveau de confiance** du marchand selon ses paiements réussis
- **`compliance-service`** : règles de fréquence et de montant, suspension automatique puis levée après un délai
- Idempotence des consommateurs, file d'erreurs (dead-letter), pattern Outbox

**Technique :** Kafka ou RabbitMQ en local, puis SQS/SNS sur AWS.

**Conteneurisation :** le broker de messages tourne en conteneur dans le Docker Compose (et LocalStack pour tester SQS/SNS sans AWS).

**Front :** statut mis à jour par polling simple (SSE en bonus).

**Critère de réussite :** avec messages perdus ou dupliqués et l'opérateur qui tombe en plein reversement, aucun paiement ne reste bloqué ni n'est doublé.

**À approfondir le moment venu :** catalogue d'événements, schéma de la saga, Outbox, stratégie de retry, tests avec pannes injectées.

---

### Projet 5 : Sécurité et observabilité

**Objectif d'apprentissage :** ce qui distingue un projet de démonstration d'un système exploitable.

**Nouvelles fonctionnalités :**
- Authentification via **Keycloak** (ou Cognito) : inscription libre, rôles acheteur / marchand, jetons OIDC/JWT validés dans chaque service
- **Authentification service à service** (client credentials), y compris vers `operator-sim`
- Limitation de débit sur la gateway
- **Journal d'audit automatique** de chaque changement d'état et de chaque décision automatique
- Statistiques marchand (volume, montants, taux de réussite) via un endpoint
- Dashboards Grafana, traces distribuées (OpenTelemetry), logs corrélés

**Conteneurisation :** Keycloak, Prometheus, Grafana et un collecteur OpenTelemetry dans le Docker Compose.

**Front :** connexion OIDC (PKCE) et un tableau de stats très simple.

**Critère de réussite :** à partir d'un paiement lent, identifier dans les traces quel service est en cause.

**À approfondir le moment venu :** configuration du realm Keycloak, validation des JWT par service, métriques clés, dashboards.

---

### Projet 6 : Kubernetes et montée en charge

**Objectif d'apprentissage :** valider le CKA par la pratique et déployer les microservices sur Kubernetes.

**Ce que fait l'application :** elle absorbe un pic d'affluence (marché, événement) où beaucoup de marchands génèrent des QR et beaucoup d'acheteurs paient en même temps.

**Nouvelles fonctionnalités :**
- Limitation de débit distribuée et cache (Redis) pour les QR et les plafonds
- Autoscaling (HPA) et mise à jour sans interruption (rolling update)
- Script de **réconciliation** : débits et reversements doivent être cohérents

**Conteneurisation et déploiement :** images de chaque service, kind/minikube en local (ils tournent eux-mêmes dans Docker), chart Helm maison, puis **une session EKS de 2 à 3 heures maximum** (images tirées depuis ECR, `terraform destroy` ensuite).

**Front :** aucun changement.

**Technique :** scénario de charge avec k6.

**Critère de réussite :** sous charge, l'autoscaling se déclenche et la réconciliation ne trouve aucun écart.

**Coût :** EKS, NAT Gateway et ALB ne sont pas gratuits : sessions courtes, budget d'alerte actif, vérification du coût estimé avant chaque lancement.

**À approfondir le moment venu :** manifestes et chart Helm, probes, HPA, scénario k6, procédure de session EKS.

---

### Projet 7 : Serverless et capstone

**Objectif d'apprentissage :** compléter la palette cloud avec le serverless et assembler l'ensemble.

**Nouvelles fonctionnalités :**
- Reversements groupés planifiés (EventBridge + Lambda)
- Réconciliation nocturne orchestrée par Step Functions
- Rapport quotidien PDF par marchand (Lambda, S3)
- Journal d'audit immuable sur DynamoDB
- Dossier d'architecture final (schémas, décisions, coûts)

**Conteneurisation :** principalement hors conteneurs (Lambda, Step Functions). Tests locaux possibles avec LocalStack.

**Front :** un lien de téléchargement du rapport (scan caméra facultatif).

**Critère de réussite :** démo filmée du parcours complet, de l'inscription marchand au rapport du lendemain.

**À approfondir le moment venu :** Lambda Java (SnapStart), modèle DynamoDB, définition Step Functions, dossier d'architecture.

---

## 9. Couverture des compétences

| Domaine | Projet(s) |
|---|---|
| Spring Boot avancé, concurrence, tests | 1 |
| Docker, Docker Compose, conteneurisation | 1 à 6 |
| AWS (S3, EC2, ECR, SES/SNS/SQS, Lambda, DynamoDB, Step Functions), Terraform, CI/CD | 2, 4, 7 |
| Microservices, résilience | 3 |
| Événementiel (saga, Outbox, idempotence, retry) | 4 |
| Sécurité (OIDC, JWT, service à service), observabilité | 5 |
| Kubernetes, EKS, scalabilité | 6 |

**Extension possible :** l'interface (port/adapter) vers l'opérateur permettrait d'ajouter plus tard un second opérateur simulé, sans modifier le cœur de l'orchestration.

---

## 10. Suivi d'avancement

À mettre à jour au fil du parcours.

| Projet | Statut | Notes |
|---|---|---|
| 1. Monolithe Spring Boot | À faire | |
| 2. Déploiement AWS | À faire | |
| 3. Microservices | À faire | |
| 4. Événementiel | À faire | |
| 5. Sécurité et observabilité | À faire | |
| 6. Kubernetes et montée en charge | À faire | |
| 7. Serverless et capstone | À faire | |

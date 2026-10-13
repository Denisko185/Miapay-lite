# MiaPay-lite : outils, technologies et langages

> **Note d'usage.** Ce fichier liste et explique tout ce dont on a besoin pour mener à bien les 7 projets de MiaPay-lite (voir `MiaPay-lite.md` pour le plan des projets). Pour chaque élément : son rôle, le ou les projets où il intervient, et des remarques utiles. Les numéros de versions ne sont volontairement pas figés : on vérifie la version stable courante au démarrage de chaque projet.

---

## 1. Langages et formats

| Langage | Rôle | Projets |
|---|---|---|
| **Java** (version LTS récente) | Langage principal de tous les services et des Lambda | 1 à 7 |
| **SQL** (dialecte PostgreSQL) | Schéma, requêtes, migrations, verrouillage | 1 à 7 |
| **TypeScript** | Front React et client d'API typé | 1 à 7 (front) |
| **HCL** | Langage de Terraform (infrastructure AWS) | 2 à 7 |
| **YAML** | Docker Compose, GitHub Actions, manifestes et charts Kubernetes, configuration Spring | 1 à 7 |
| **JSON** | Contrats d'API, événements, jetons JWT, configuration | 1 à 7 |
| **Bash** | Scripts d'automatisation, commandes CLI, lancement des tests de charge | 1 à 7 |
| **Markdown** | README, documentation, ADR (décisions d'architecture) | 1 à 7 |

---

## 2. Backend : Java et écosystème Spring

| Outil | Rôle | Projets |
|---|---|---|
| **Spring Boot** | Framework de base : démarrage, configuration, auto-configuration | 1 à 7 |
| **Spring Web (MVC)** | API REST | 1 à 7 |
| **Spring Data JPA / Hibernate** | Accès aux données, verrouillage optimiste (`@Version`) et pessimiste | 1 à 7 |
| **Spring Security** | Authentification, autorisation, validation des JWT (resource server) | 1, 5 |
| **Bean Validation** | Validation des entrées | 1 à 7 |
| **Spring Boot Actuator** | Endpoints de santé et de métriques | 1 à 7 |
| **Spring Scheduling** (+ ShedLock si plusieurs instances) | Tâches planifiées : expiration des QR, réconciliation | 1, 4, 6 |
| **springdoc-openapi** | Génère la documentation OpenAPI et Swagger UI | 1 à 7 |
| **Spring Cloud Gateway** | API Gateway (routage, limitation de débit) | 3, 5, 6 |
| **Resilience4j** | Timeout, retry, circuit breaker, bulkhead | 3, 4 |
| **Spring for Apache Kafka** ou **Spring AMQP** | Production et consommation de messages | 4 |
| **Spring Cloud AWS** (ou AWS SDK for Java v2) | Accès S3, SQS, SNS, SES, DynamoDB depuis Spring | 2, 4, 7 |
| **Spring Cloud OpenFeign** ou `RestClient` | Appels HTTP entre services | 3 |
| **Micrometer** | Métriques applicatives exposées à Prometheus | 5 |
| **Flyway** | Migrations de base de données versionnées | 1 à 7 |
| **ZXing** | Génération de l'image du QR code | 2 |
| **OpenPDF** (ou équivalent) | Génération des reçus et rapports PDF | 2, 7 |
| **AWS Lambda Java** (runtime + SnapStart) | Fonctions serverless | 7 |

---

## 3. Build, dépendances et qualité

| Outil | Rôle |
|---|---|
| **Maven** (ou **Gradle**) | Build, gestion des dépendances, modules multi-services |
| **SDKMAN** | Installation et changement des versions de Java |
| **Git / GitHub** | Versionnement, dépôt public pour le portfolio |
| **GitHub Actions** | Intégration et déploiement continus (projet 2 en premier) |
| **Checkstyle / Spotless** (optionnel) | Style de code homogène |
| **JaCoCo** (optionnel) | Mesure de couverture de tests |

---

## 4. Tests

| Outil | Rôle | Projets |
|---|---|---|
| **JUnit 5** | Framework de tests | 1 à 7 |
| **Mockito** | Doublures pour les tests unitaires | 1 à 7 |
| **AssertJ** | Assertions lisibles | 1 à 7 |
| **Testcontainers** | Vrais PostgreSQL, Kafka, LocalStack dans les tests d'intégration (Docker requis) | 1 à 6 |
| **Awaitility** | Attendre un résultat asynchrone dans un test | 4 |
| **WireMock** | Simuler un service externe (par exemple l'opérateur) | 3 |
| **Spring Cloud Contract** ou **Pact** (optionnel) | Tests de contrat entre services | 3 |
| **k6** | Tests de charge (scénario d'affluence) | 5, 6 |
| **Tests de concurrence maison** (`ExecutorService`, `CountDownLatch`) | Valider l'usage unique du QR et l'idempotence | 1 |

---

## 5. Données

| Outil | Rôle | Projets |
|---|---|---|
| **PostgreSQL** | Base relationnelle de chaque service (une base par service à partir du projet 3) | 1 à 6 |
| **Redis** | Cache, limitation de débit distribuée | 6 |
| **Amazon DynamoDB** | Journal d'audit immuable | 7 |
| **Amazon RDS for PostgreSQL** (optionnel) | PostgreSQL managé, selon ce que permet l'offre gratuite du compte | 2 |

---

## 6. Messagerie et événements

| Outil | Rôle | Projets |
|---|---|---|
| **Apache Kafka** ou **RabbitMQ** | Broker de messages en local (au choix, à trancher au projet 4) | 4, 5, 6 |
| **Amazon SQS** | Files d'attente sur AWS, dead-letter queues | 4 |
| **Amazon SNS** | Diffusion d'événements sur AWS | 4 |
| **Amazon EventBridge** | Déclenchements planifiés | 7 |
| **Patterns** : Outbox, saga, idempotence, dead-letter queue | Garantir la cohérence malgré pannes et doublons | 4 |

---

## 7. Conteneurs et Kubernetes

| Outil | Rôle | Projets |
|---|---|---|
| **Docker** | Conteneurisation des services | 1 à 6 |
| **Dockerfile multi-stage** | Images Java légères | 1 à 6 |
| **Docker Compose** | Lancer le système complet en local (bases, services, broker, Keycloak, observabilité) | 1 à 5 |
| **LocalStack** | Émuler des services AWS en local (SQS, SNS, S3, DynamoDB) | 4, 7 |
| **Amazon ECR** | Registre d'images sur AWS | 2, 6 |
| **kind** ou **minikube** | Cluster Kubernetes local | 6 |
| **kubectl** | Pilotage du cluster | 6 |
| **Helm** | Chart de déploiement de la plateforme | 6 |
| **Kubernetes** : Deployments, Services, ConfigMaps, Secrets, Ingress, probes, HPA | Déploiement et mise à l'échelle | 6 |
| **Amazon EKS** | Kubernetes managé, en sessions courtes uniquement | 6 |
| **Argo CD** (optionnel) | Déploiement GitOps | 6 |

---

## 8. Services AWS

| Service | Rôle dans MiaPay-lite | Projets |
|---|---|---|
| **IAM** | Rôles et permissions à moindre privilège | 2 à 7 |
| **S3** | Images de QR, reçus et rapports PDF, hébergement du front | 2, 7 |
| **CloudFront** | Distribution du front en HTTPS | 2 |
| **EC2** | Exécution des conteneurs au projet 2 | 2 |
| **ECR** | Registre d'images | 2, 6 |
| **SES** (ou SNS) | E-mails de confirmation | 2, 4 |
| **SQS / SNS** | Messagerie événementielle | 4 |
| **EventBridge** | Déclencheurs planifiés | 7 |
| **Lambda** | Reversements groupés, rapports PDF | 7 |
| **Step Functions** | Réconciliation nocturne | 7 |
| **DynamoDB** | Journal d'audit | 7 |
| **CloudWatch** | Logs et alarmes | 2 à 7 |
| **EKS** | Kubernetes managé, sessions courtes | 6 |
| **VPC** | Réseau (sous-réseaux, groupes de sécurité) | 2, 6 |
| **AWS Budgets** | Alerte de dépense, à créer dès le départ | 2 à 7 |
| **AWS CLI** | Pilotage depuis le terminal (déjà installé dans Codespaces) | 2 à 7 |
| **Cognito** (alternative à Keycloak) | Authentification managée | 5 |

---

## 9. Infrastructure as Code et CI/CD

| Outil | Rôle | Projets |
|---|---|---|
| **Terraform** | Décrire toute l'infrastructure AWS et la détruire proprement (`terraform destroy`) | 2 à 7 |
| **GitHub Actions** | Build, tests, construction et publication d'images, déploiement | 2 à 7 |
| **Makefile** ou scripts Bash | Raccourcis pour les commandes récurrentes | 1 à 7 |

---

## 10. Sécurité

| Élément | Rôle | Projets |
|---|---|---|
| **OAuth 2.0 / OpenID Connect** | Standard d'authentification et d'autorisation | 5 |
| **JWT** | Jetons signés validés par chaque service | 1, 5 |
| **Authorization Code + PKCE** | Connexion de la PWA | 5 |
| **Client credentials** | Authentification de service à service | 5 |
| **Keycloak** (ou Cognito) | Fournisseur d'identité | 5 |
| **Limitation de débit** | Protection contre les abus (gateway puis Redis) | 5, 6 |
| **Gestion des secrets** (variables d'environnement, Secrets Kubernetes, AWS Secrets Manager si le coût le permet) | Ne jamais commiter de secrets | 2 à 7 |

---

## 11. Observabilité

| Outil | Rôle | Projets |
|---|---|---|
| **Micrometer** | Métriques depuis Spring | 5 |
| **Prometheus** | Collecte des métriques | 5, 6 |
| **Grafana** | Dashboards | 5, 6 |
| **OpenTelemetry** | Traces distribuées et corrélation des logs | 5 |
| **Jaeger** ou **Tempo** | Visualisation des traces | 5 |
| **Logs structurés (JSON)** avec identifiant de trace | Retrouver un paiement de bout en bout | 5 |

---

## 12. Front (minimaliste)

| Outil | Rôle |
|---|---|
| **React + TypeScript** | PWA de 4 écrans |
| **Vite** | Outil de build et serveur de développement |
| **React Router** | Navigation |
| **TanStack Query** | Appels API et rafraîchissement (polling du statut) |
| **Tailwind CSS** et **shadcn/ui** | Composants prêts à l'emploi, sans design sur mesure |
| **openapi-typescript** | Client d'API typé généré depuis l'OpenAPI de Spring |
| **oidc-client-ts** (ou équivalent) | Connexion OIDC avec PKCE (projet 5) |
| **Service worker / manifest PWA** | Application installable |
| **Bibliothèque de scan de QR** (optionnelle) | Scan par la caméra, facultatif |

---

## 13. Environnement de développement

| Outil | Rôle |
|---|---|
| **GitHub Codespaces** | Environnement cloud (60 heures gratuites par mois) |
| **Poste local avec Docker** | Plus confortable dès que le Compose contient beaucoup de conteneurs (projets 4 et 5) |
| **IntelliJ IDEA** ou **VS Code** | IDE |
| **Swagger UI** | Tester les API sans écrire de front |
| **Postman**, **curl** ou **HTTPie** | Requêtes manuelles |
| **Un client SQL** (DBeaver, par exemple) | Inspecter les bases |
| **devcontainer** (`.devcontainer/devcontainer.json`) | Environnement reproductible dans Codespaces |

---

## 14. Coûts et free tier AWS

- **Vérifier le type de compte** : les comptes créés avant le 15 juillet 2025 restent sur l'ancien Free Tier (12 mois gratuits et services Always Free). Les comptes plus récents reçoivent 100 $ de crédits, extensibles à 200 $ avec des activités d'onboarding, pour une durée de 6 mois ou jusqu'à épuisement des crédits. Le solde et les conditions se consultent dans la console AWS, rubrique Free Tier et Billing.
- **Services Always Free utiles ici** (dans des limites mensuelles) : Lambda, DynamoDB, S3 (5 Go), CloudFront, SNS. SQS a aussi une offre gratuite mensuelle.
- **Non gratuits** : EKS (control plane facturé à l'heure), NAT Gateway, Application Load Balancer, et EC2 ou RDS hors période gratuite. Pour ceux-là : sessions de quelques heures, jamais du 24/7.
- **SES** : démarre en mode sandbox (destinataires à vérifier), quotas et gratuité à vérifier.
- **Cognito** : offre gratuite à vérifier avant de s'y engager, Keycloak en local n'a aucun coût.
- **Habitudes indispensables** : créer un **AWS Budget à 1 €** dès le départ, tout décrire en Terraform, **détruire** les ressources après chaque session, supprimer les ressources de test (par exemple les hosted zones Route 53 qui ne sont pas gratuites).

---

## 15. Règle de travail : local d'abord, AWS en sessions courtes

Développer et tester en local (Docker Compose, Testcontainers, LocalStack, kind), puis déployer sur AWS seulement pour valider, et détruire ensuite. Les projets 1 à 5 se font presque entièrement en local, ce qui est à la fois la partie la plus rentable et la moins risquée côté facture.

---

## 16. Ordre d'apprentissage conseillé

| Étape | À maîtriser avant d'attaquer |
|---|---|
| Avant le projet 1 | Java et Spring Boot de base, Docker (images, volumes, réseaux), Docker Compose, PostgreSQL |
| Avant le projet 2 | Bases AWS du SAA (IAM, S3, EC2, VPC), Terraform de base, GitHub Actions |
| Avant le projet 3 | Appels HTTP entre services, notions de résilience (timeout, retry, circuit breaker) |
| Avant le projet 4 | Concepts de messagerie (producteur, consommateur, partitions ou files), idempotence, Outbox |
| Avant le projet 5 | OAuth 2.0 / OIDC, JWT, métriques et traces |
| Avant le projet 6 | Kubernetes de base (préparation du CKA), Helm, notions d'autoscaling |
| Avant le projet 7 | Lambda, DynamoDB, Step Functions |

---

## 17. Ressources de référence

- Documentation officielle : Spring, AWS, Terraform, Kubernetes, Docker, Keycloak, Testcontainers.
- **Baeldung** : référence Spring en profondeur.
- **KodeKloud** : labs Kubernetes (préparation du CKA).
- **Tutorials Dojo** : examens blancs AWS (déjà en usage pour le SAA).
- **Microservices Patterns** (Chris Richardson) et **Building Microservices** (Sam Newman) : patterns de microservices, saga, Outbox.
- **Designing Data-Intensive Applications** (Martin Kleppmann) : cohérence, messagerie, transactions distribuées.

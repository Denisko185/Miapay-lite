# Miapay-lite

Spring Boot (Java 21) API backed by PostgreSQL. The schema is managed by Flyway; Hibernate only validates it (`ddl-auto: validate`).

## Prerequisites

- Java 21
- Docker (local PostgreSQL and Testcontainers-based tests)

Maven does not need to be installed: the `./mvnw` wrapper is included.

## Repository layout

- `backend/`: the Spring Boot application, its `docker-compose.yml` and `.env`. All commands below are run from this folder.
- `docs/`: project plans and technical documentation (in French).

## Running the project

1. Create the `.env` file from the template (it is ignored by Git):

   ```bash
   cd backend
   cp .env.example .env
   ```

   | Variable            | Purpose                | Default     |
   |---------------------|------------------------|-------------|
   | `POSTGRES_DB`       | Database name          | (required)  |
   | `POSTGRES_USER`     | PostgreSQL user        | (required)  |
   | `POSTGRES_PASSWORD` | PostgreSQL password    | (required)  |
   | `POSTGRES_HOST`     | Database host          | `localhost` |
   | `POSTGRES_PORT`     | Database port          | `5432`      |
   | `PGADMIN_EMAIL`     | pgAdmin login (Compose)    | (required)  |
   | `PGADMIN_PASSWORD`  | pgAdmin password (Compose) | (required)  |

2. Start PostgreSQL (Docker Compose reads `.env` automatically):

   ```bash
   docker compose up -d
   ```

3. Export the variables into your shell, then start the application:

   ```bash
   set -a; source .env; set +a
   ./mvnw spring-boot:run
   ```

   > Spring Boot does not read `.env` on its own. Without the export, the application receives the literal text `${POSTGRES_USER}` and the connection fails with an authentication error.

On startup, Flyway applies the migrations in `backend/src/main/resources/db/migration`.

## Checking that it works

- Health: <http://localhost:8080/actuator/health>
- Info: <http://localhost:8080/actuator/info>
- OpenAPI docs: <http://localhost:8080/swagger-ui.html>

Actuator only exposes `health` and `info`.

To inspect the database (the role is `POSTGRES_USER`, not the database name):

```bash
docker compose exec postgres psql -U admin -d miapay -c '\d users'
```

## Tests

```bash
./mvnw test
```

Integration tests start their own PostgreSQL through Testcontainers, so they depend neither on `.env` nor on the Compose container, but Docker must be running.

To run the application against a throwaway Testcontainers database, launch `TestMiapayliteApplication` (in `backend/src/test/java`) from your IDE.

## Resetting the local database

PostgreSQL only applies `POSTGRES_USER` and `POSTGRES_PASSWORD` when the volume is first created. After changing these values:

```bash
docker compose down -v && docker compose up -d
```

This deletes all local data.

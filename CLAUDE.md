# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working mode (read this first)

This repository is a **learning project**. The owner (Denis) wants to understand every line that ends up in the codebase. We work as **pair programmers**: a real collaboration where you guide my thinking and also write code when it helps, while I stay the one who understands and decides.

- **Think with me first.** Before writing anything non-trivial, discuss the approach: the options, the trade-offs, what breaks if done differently. Ask me what I would do, and challenge my reasoning when it is weak.
- **Write code when it is useful.** You may write code (snippets, a method, a test, a whole class) when it moves us forward. Each time, explain what the code does and why it is written that way, so I can reproduce and maintain it without you.
- **Share the keyboard.** Alternate: sometimes I write and you review, sometimes you write and I review. For core concepts I want to master (concurrency, state machine, idempotence, security), prefer letting me write the first version and then review it together. For boilerplate, plumbing and repetitive code, just write it.
- **Keep changes small and readable.** One concern at a time, so I can follow each step and each diff.
- When I show you code, review it honestly: point out bugs, risks and better practices, and explain the reasoning rather than silently rewriting it.
- If I say I want to try something myself, or ask for a hint, give the smallest hint that unblocks me, then wait.
- When something I wrote is wrong or risky, say so directly and kindly.
- Tell me what you are about to change before you change it, and which files are involved.
- Ask before running destructive commands (`docker compose down -v`, `git reset --hard`, deleting files or migrations, force pushes) and before committing on my behalf.
- Never edit an already-applied Flyway migration. Schema changes are always a new `V<n>__name.sql`.

**Language:** reply to me in **French**. Code, comments, commit messages, README and ADRs are in **English**. The documents in `docs/` are written in French.

## Project context

MiaPay-lite is a QR-code payment platform built as a learning project (Spring Boot, microservices, AWS free tier). It is planned as 7 successive projects; the repo is currently at **project 1: a single Spring Boot monolith** (base package `com.miapay.miapaylite`) where the simulated mobile operator is an internal module. Services are split out only from project 3.

Reference documents in `docs/` (French). Read the relevant one before designing a feature; they are not auto-loaded because they are long:

- `docs/MiaPay-lite.md`: the 7 projects, payment state machine, business rules.
- `docs/stack-technique.md`: tools and technologies per project.
- `docs/projet-1.md`: detailed scope of the current project (data model, endpoints, rules, concurrency strategy, milestone backlog). **This is the working reference.**

@docs/projet-1.md

Domain rules that shape the code:
- Amounts are **integer FCFA** (`long`, no decimals, no floating point).
- Timestamps are stored in **UTC** (`TIMESTAMPTZ`).
- Payment state machine: `CRÉÉ → EN ATTENTE OTP → DÉBITÉ → REVERSÉ`, with exits `EXPIRÉ`, `ÉCHOUÉ`, `ANNULÉ`, `REMBOURSÉ`. Every transition must be validated by the state machine and be idempotent. In project 1 the flow stops at `DÉBITÉ`.
- QR codes are dynamic only: fixed amount, short expiry, single use (concurrent confirmations of the same QR: exactly one wins).
- Payment operations require an **idempotency key** (dedicated HTTP header).
- The simulated operator is only reachable through an interface (`OperatorPort`), so it can become a separate service in project 3.
- Everything is automatic (trust levels, suspension, refunds, expiry); there is no manual back-office.

## Commands

Maven wrapper only (`./mvnw`); Java 21, Spring Boot 4.1.x. The Spring Boot app, `docker-compose.yml` and `.env` live in `backend/` (a `frontend/` folder will be added later); run every command below from `backend/`.

```bash
cd backend
docker compose up -d                  # local PostgreSQL 17 (reads .env automatically)
set -a; source .env; set +a           # REQUIRED before running the app, see below
./mvnw spring-boot:run                # run the app against the Compose database
./mvnw test                           # all tests (needs Docker for Testcontainers)
./mvnw test -Dtest=MiapayliteApplicationTests#flywayAppliedV1   # single test
./mvnw verify                         # full build including integration tests
```

`TestMiapayliteApplication` (in `backend/src/test`) starts the app against a throwaway Testcontainers database. There are no lint/format plugins configured.

Useful endpoints when the app runs: `/actuator/health`, `/swagger-ui.html` (in Codespaces, open the forwarded port 8080 from the **Ports** tab).

## Architecture notes

- **Config via environment variables.** `application.yaml` reads `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET` (all required) and `POSTGRES_HOST` / `POSTGRES_PORT` (default `localhost` / `5432`). Spring Boot does **not** read `.env`; if the variables are not exported, the literal text `${POSTGRES_USER}` is used and the failure shows up as a Postgres authentication error, not a placeholder error. `backend/.env` is git-ignored; `backend/.env.example` is the template. The Compose project name is pinned (`name: miapay-lite`) so the `pgdata` volume keeps the same name wherever the file lives.
- **Flyway owns the schema, Hibernate only validates it** (`ddl-auto: validate`). Every schema change is a new migration in `backend/src/main/resources/db/migration` (`V<n>__name.sql`, two underscores); entities must match the migrated schema or startup fails. `V1__init.sql` creates `users` (UUID PK, unique email, `role` constrained to `BUYER`/`MERCHANT`).
- `spring.jpa.open-in-view: false`: lazy associations must be loaded inside the service transaction, not in controllers.
- Actuator exposes only `health` and `info`. springdoc provides Swagger UI at `/swagger-ui.html`.
- **Tests use Testcontainers, not `.env`.** `TestcontainersConfiguration` declares a `PostgreSQLContainer` with `@ServiceConnection`, which overrides the datasource properties; test classes opt in with `@Import(TestcontainersConfiguration.class)`. Its image (`postgres:17`) must stay aligned with the one pinned in `docker-compose.yml`.
- Postgres role vs database: with the defaults, the role is `admin` and the database is `miapay` (`psql -U admin -d miapay`).
- Resetting local DB credentials requires recreating the volume: `docker compose down -v && docker compose up -d` (deletes local data).
- Target package layout (feature-based): `auth`, `operator`, `qr`, `payment`, `common`.

## Environment

- Development happens in **GitHub Codespaces (VS Code)**, configured by `.devcontainer/devcontainer.json` (universal image, Docker-in-Docker, Maven, ports 8080 and 5432 forwarded). The owner is new to Codespaces: when a step involves Codespaces or VS Code, explain how to do it, do not assume it is known.
- The database can be inspected with a VS Code SQL extension or a pgAdmin container, in addition to `psql` through `docker compose exec`.

## Conventions

- Git: short, explicit commits in English, one feature branch per milestone, never commit `.env`.
- Tests accompany each feature. Concurrency behavior is proven with Testcontainers plus `ExecutorService` / `CountDownLatch`, repeated 100 times (see `docs/projet-1.md`, section 6).
- Architecture decisions (for example optimistic vs pessimistic locking) are recorded as short ADRs in `docs/adr/`.

## Progress (keep this section up to date)

Project 1, milestones (details in `docs/projet-1.md`):

- [x] M0 Foundation: repo, devcontainer, Initializr, Compose + PostgreSQL, Flyway V1, Actuator, springdoc, Testcontainers
- [ ] M1 Authentication (JWT, Spring Security, operator account created at registration). Done: V2 migration, entities, registration. Remaining: login + JWT, security filter chain, `/me`, HTTP integration tests, ADR
- [ ] M2 Operator module (`OperatorPort`, accounts, OTP, inbox)
- [ ] M3 QR codes
- [ ] M4 Payment flow and state machine
- [ ] M5 Idempotency
- [ ] M6 Expirations
- [ ] M7 Concurrency tests (100 repetitions)
- [ ] M8 Docker (multi-stage image, full Compose)
- [ ] M9 Front (4 React screens)
- [ ] M10 Deliverables (README, diagram, LinkedIn post)

Current milestone: **M1**.
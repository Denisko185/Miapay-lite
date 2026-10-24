# ADR 0001: Stateless JWT authentication (HS256) and BCrypt password hashing

- Status: Accepted
- Date: 2026-10-24
- Milestone: M1 (Authentication)

## Context

MiaPay-lite needs to identify two kinds of users (buyers and merchants) on a REST API. Today it is a single Spring Boot application, but it will be split into several services from project 3, and Keycloak is planned for project 5. The decision must be simple enough for a monolith without painting the project into a corner.

## Decision

1. **Stateless authentication with JWT Bearer tokens.** `POST /auth/login` returns an access token that the client sends in `Authorization: Bearer <token>`. The server keeps no session.
2. **Tokens are signed with HS256**, using a shared secret read from the `JWT_SECRET` environment variable (at least 32 bytes; the application refuses to start otherwise). The token carries `sub` (user id), `role`, `iat` and `exp` (lifetime: 1 hour, `app.jwt.ttl`).
3. **Passwords are hashed with BCrypt** through Spring's `DelegatingPasswordEncoder`. Hashes are stored with their `{bcrypt}` prefix so the algorithm can change later without invalidating existing hashes. Passwords are limited to 72 characters, because BCrypt ignores anything beyond that.
4. **CSRF protection is disabled and no HTTP session is created.** This is safe only because the token is added explicitly by the client in a header, not sent automatically by the browser like a cookie.
5. **The identity of the caller always comes from the validated token**, never from a request parameter or body (see `GET /me`). A test guards this rule.
6. **Login gives no hint about which part failed.** An unknown email and a wrong password return the same 401, and take about the same time: when the user does not exist, the password is still checked against a precomputed dummy hash.

## Alternatives considered

| Alternative | Why not now |
|---|---|
| Server-side sessions with a cookie | Needs CSRF protection and a shared session store as soon as there are several instances or services |
| RS256 (private key signs, public key verifies) | Better when several services only need to verify tokens, but it adds key generation and rotation work that a monolith does not need yet |
| Opaque tokens with introspection | Every request would call the issuer, and there is no separate issuer before project 5 |
| Keycloak or Cognito right away | Too early: it would hide the mechanics this project is meant to teach |
| Argon2 instead of BCrypt | Needs an extra library (BouncyCastle) and parameter tuning, for little gain here |

## Consequences

- **No revocation.** A stolen token stays valid until it expires (1 hour). There is no server-side logout and no refresh token.
- **The secret is a single point of failure.** Anyone with `JWT_SECRET` can forge tokens for any user, and every service that verifies tokens would have to share it. This is acceptable in a monolith and is the main reason to move to RS256 or Keycloak when services are split (project 5).
- **Tokens must travel over HTTPS only** and must never be logged.
- **Registration reveals whether an email exists** (409 on a duplicate). This is accepted as a usability trade-off; login does not reveal it.
- **No rate limiting yet**, so password guessing is only slowed by BCrypt's cost. Rate limiting is planned for projects 5 and 6.
- Role-based rules (`hasRole`) are not configured yet: the `role` claim is already mapped to `ROLE_BUYER` / `ROLE_MERCHANT`, and the rules will be added together with their endpoints (`/qr`, `/payments`).

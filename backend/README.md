# Sneakster backend

A small Ktor service that stores and serves the leaderboard, plus a shared
effects pool players can leave gifts (or mischief) in for each other. No
accounts — a score submission is just a nickname, a score, and a difficulty.

## Stack

- Kotlin + [Ktor](https://ktor.io/) (Netty engine)
- [Exposed](https://github.com/JetBrains/Exposed) over PostgreSQL via HikariCP
- kotlinx.serialization for JSON

## API

| Method | Path                 | Body / query                              | Notes |
|--------|----------------------|--------------------------------------------|-------|
| GET    | `/health`            | —                                          | used by the Docker healthcheck |
| GET    | `/api/v1/leaderboard`| `?limit=20&difficulty=easy\|normal\|hard`  | top scores, highest first |
| POST   | `/api/v1/scores`     | `{"nickname","score","difficulty"}`        | rate-limited to 10/min per IP |
| POST   | `/api/v1/pool/contribute` | `{"nickname","effectType","deviceId"}`| adds one pending effect to the shared pool; rate-limited to 20/min per IP, plus a per-device cap below |
| POST   | `/api/v1/pool/pull`  | —                                          | pops one random pending effect (204 if the pool is empty); same rate limit |

Nicknames are 1–20 characters (letters, numbers, space, `-`, `_`); scores are
capped at a plausible maximum server-side. This is basic sanity-checking,
not real anti-cheat — treat the leaderboard as casual, not competitive.

`effectType` must be one of `SHARED_GIFT`, `SHARED_PRANK`, `SHARED_SHIELD`,
or `SHARED_FOG` (kept in sync with the Android app's pool-exclusive
`PowerUpType` entries — see `model/PoolDto.kt`). The server doesn't track
token balances at all; the
client spends its own locally-persisted tokens before calling contribute,
same trust model as the rest of this casual, no-auth backend. A pull is
consumed the instant it's handed out, so it can never be given to two
different players.

`deviceId` is a UUID the Android app generates once on first launch and
persists locally — not an account, no login, nothing else is ever looked up
by it. It exists purely so the server can cap contributions per install
(10 per rolling hour, see `PoolRepository.MAX_CONTRIBUTIONS_PER_DEVICE`)
independently of the per-IP rate limit above, since a NAT'd household or a
scripted client hammering the IP-based limit alone could otherwise flood
the pool with one effect type. A device over its cap gets `429 Too Many
Requests`.

## Configuration

Everything is environment-variable driven (see `src/main/resources/application.conf`):

| Variable              | Default                                      |
|-----------------------|-----------------------------------------------|
| `PORT`                | `8080`                                        |
| `DATABASE_JDBC_URL`   | `jdbc:postgresql://localhost:5432/sneakster`  |
| `DATABASE_USER`       | `sneakster`                                   |
| `DATABASE_PASSWORD`   | `sneakster`                                   |
| `ALLOWED_ORIGINS`     | empty (allows any origin — fine for a phone-only client with no browser Origin header) |

## Running locally

```bash
./gradlew run
```

Needs a reachable Postgres; easiest is `docker compose up postgres` from the
repo root, then run the app locally against `localhost:5432`.

## Tests

```bash
./gradlew test
```

Validation rules run against plain JVM assertions; the repository/ranking
logic runs against an in-memory H2 database so no Postgres is needed to test.

## Docker

```bash
docker build -t sneakster-api .
```

The `Dockerfile` is a two-stage build: Gradle builds a fat jar in a JDK
image, then a slim JRE image runs it as a non-root user with a
`curl`-based healthcheck against `/health`. See the repo root's
`docker-compose.yml` for wiring this up with Postgres.

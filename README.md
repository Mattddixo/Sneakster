# Sneakster

A snake game that isn't locked to a grid. The snake always moves forward and
turns with two on-screen buttons; it picks up speed the longer you survive,
bounces off the arena's edges instead of dying there, and grows a longer tail
the faster it goes. Glowing pickups and temporary obstacles keep runs from
feeling the same twice. Scores go to a small self-hosted leaderboard.

- **`android/`** — the game itself: a Kotlin + Jetpack Compose app.
- **`backend/`** — the leaderboard API: Kotlin + Ktor + PostgreSQL, meant to
  run on a homelab server reachable over Tailscale.

## How a round works

- Two buttons at the bottom of the screen are the entire control surface:
  hold left or right to turn, let go to go straight.
- Speed ramps up automatically over the course of a run (how fast, and how
  high it ramps, depends on the difficulty you pick in Settings) — the
  challenge comes from a ticking clock, not a sudden jump.
- Hitting the arena's edge bounces you back in instead of ending the run.
- The snake's tail is longer at higher speed. Touching your own tail ends
  the run.
- Glowing circles are optional pickups: speed up, slow down, a few seconds
  of slow motion, or — the risky one — dropping fresh obstacles onto the
  field in exchange for bonus points.
- Obstacles disappear after several seconds (flashing a countdown ring as
  they run out); running into one ends the run just like your own tail does.
- Everything in a match is configurable ahead of time from Settings:
  difficulty, turn sensitivity, sound, haptics, and the leaderboard
  server's address.

## Repo layout and why it's split this way

The `engine` module (`android/engine`) holds the entire simulation — movement,
wall bounces, collisions, pickups, obstacle lifetimes, scoring — as plain
Kotlin with no Android dependency. That's what `android/engine/src/test`
exercises directly with JUnit 5, independent of an emulator or device. The
`app` module (`android/app`) is Compose UI, a thin `GameViewModel` that
drives the engine once per frame, and the networking/settings glue. See
`android/README.md` for the full breakdown and how to build it.

`backend/` is a small, separately-deployable Ktor service; see
`backend/README.md` for its API and how it's structured.

## Running the backend on your homelab

The backend is meant to sit on a machine your Tailscale tailnet can reach —
the Android app just needs a host:port in Settings.

```bash
cp .env.example .env   # set a real POSTGRES_PASSWORD
docker compose up --build -d
```

This starts Postgres and the API, published on host port `8081` by default
(the container always listens on `8080` internally; override with
`HOST_PORT` in `.env` if you need a different host port). Point the
Android app's Settings screen at `<your-tailscale-hostname-or-ip>:8081`.

The `ports:` mapping in `docker-compose.yml` binds `8081` on every
interface by default, which is fine on a box only reachable over
Tailscale; if this host is also reachable some other way, bind to the
tailnet interface's IP specifically instead (e.g.
`"100.x.x.x:8081:8080"`).

## Building the Android app

See `android/README.md` — you'll need Android Studio (or the command-line
SDK) with API 34 installed, since this environment doesn't have the Android
SDK available to build or run it.

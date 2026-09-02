# Sneakster

A top-down driving game that isn't locked to a grid. Your vehicle always
moves forward and turns with two on-screen buttons, picking up speed the
longer you survive and bouncing off the arena's edges instead of dying
there. Obstacles are the whole point: hit one anywhere but its exposed back
and the run ends, but ram it from behind and it's destroyed for a solid
score bonus. Glowing pickups and a shared effects pool keep runs from
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
- Obstacles are the core of the game: each one has an exposed "back," shown
  as a distinctly colored wedge on the obstacle itself. Hit it anywhere else
  and the run ends; ram it from that back wedge and it's destroyed instead,
  awarding a solid score bonus on top of the points you're already earning
  just for staying alive. Obstacles don't expire on their own — they sit
  there until you destroy one or the run ends. New ones never spawn right
  on top of you — they keep a wide berth so a fresh obstacle is never the
  reason you didn't have time to react.
- Glowing circles are optional pickups, kept to a small, easy-to-read set:
  slow down, a shield charge, tokens (the currency for the Shop, below),
  and a rare one that rotates the whole arena another 45° clockwise every
  time you collect it — clipped rather than shrunk to fit, so it reads as
  an octagon partway through the turn. It sticks at whatever rotation it's
  on until the next one nudges it further; it doesn't wear off on its own.
  A fifth color marks anything pulled from the shared effects pool (see
  below) — deliberately one color for all of them, since which specific
  effect you got is meant to be a surprise.
- A shield charge turns a bad hit into a bounce-off instead of ending the
  run (with a brief moment of invincibility so you don't immediately eat
  another one) — up to two at a time. Find them as pickups, or earn one for
  free every few obstacles you destroy in a row, so playing the core loop
  well is what keeps you safe, not luck.
- Everything in a match is configurable ahead of time from Settings:
  difficulty, turn sensitivity, sound, haptics, and the leaderboard
  server's address.

## Tokens and the shared effects pool

Small token pickups appear on the board too. Tokens are spent from the Shop
screen to leave an effect in a pool shared across everyone playing against
that backend — not for yourself. The Shop is split into two sections, priced
to be affordable off a single decent run rather than needing a grind: things
that help whoever finds them (a Shield charge, a Speed Boost) and things that
hinder them (an Ambush of extra obstacles, a Fog that dims their board) —
each still worth bonus points either way. Each round, sometime in its first
two minutes, the game pulls one pending effect from that pool (if there is
one) and drops it into your match — as one of the five orb colors above, so
you won't know which of the four it is until you collect it. It's a way for
players to leave each other something without ever knowing who it came from.
See `backend/README.md` for the pool's API
and trust model.

## Repo layout and why it's split this way

The `engine` module (`android/engine`) holds the entire simulation — movement,
wall bounces, obstacle collision and destruction, pickups, scoring — as plain
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

By default this binds `8081` on every interface, which is fine on a box
only reachable over Tailscale. If this host is also reachable some other
way, set `TAILSCALE_IP` in `.env` to your `tailscale ip -4` output to
restrict the published port to the tailnet interface only.

## Building the Android app

See `android/README.md` — you'll need Android Studio (or the command-line
SDK) with API 34 installed, since this environment doesn't have the Android
SDK available to build or run it.

## License and privacy

Sneakster is MIT-licensed — see `LICENSE`. See `PRIVACY.md` for exactly
what data the app and backend collect (a nickname, a per-install device ID,
and IP addresses used transiently for rate-limiting — no accounts, no
analytics, no third-party tracking).

# Sneakster Android app

Kotlin + Jetpack Compose. Two Gradle modules:

- **`engine/`** — the entire game simulation as plain Kotlin (no Android
  dependency): movement, wall bounces, self/obstacle collision, pickups,
  obstacle lifetimes, scoring, difficulty ramps. Fully unit-tested with
  JUnit 5 (`./gradlew :engine:test`) — this is the part correctness bugs
  would actually live in, and it runs on any JVM without an emulator.
- **`app/`** — the Compose UI, on-screen controls, a `GameViewModel` that
  drives one `engine` tick per frame via `withFrameNanos`, and the
  networking/settings glue.

## Building

Requires Android Studio (or the command-line SDK) with **compileSdk 34**
installed, and a JDK 17+. This wasn't built or run against a device or
emulator while writing it — the sandbox this was developed in has no
Android SDK, only a plain JDK, so only `:engine`'s pure-Kotlin logic could
actually be compiled and tested here (`./gradlew :engine:test` — 17/17
passing). The `app` module's Compose code was written and reviewed
carefully, but treat first-build teething issues (a missing SDK component,
a dependency version bump) as more likely here than in `engine`.

```bash
./gradlew assembleDebug          # build the APK
./gradlew installDebug           # install on a connected device/emulator
./gradlew :engine:test           # run the engine's unit tests
```

To point a debug build at your homelab server without opening Settings
every time:

```bash
./gradlew assembleDebug -PserverBaseUrl=http://100.x.x.x:8080
```

(Settings screen also lets you set/override this at runtime — it's stored
per-install via DataStore, independent of this build-time default.)

## Architecture notes

- **No DI framework.** `AppContainer` (in `app/src/main/kotlin/.../di/`) is
  a handful of manually-constructed singletons — Hilt/Koin would be pure
  overhead for an app this size.
- **No shared Gradle module with the backend.** The leaderboard DTOs
  (`ScoreSubmission`, `LeaderboardEntry`, `ScoreSubmissionResult`) and pool
  DTOs (`PoolContributionRequest`, `PulledEffect`) are duplicated in
  `app/src/main/kotlin/.../data/LeaderboardModels.kt` and `PoolModels.kt`
  rather than wiring up a composite build for a handful of data classes.
- **Ktor Client**, not Retrofit, for networking — it mirrors the backend's
  own stack (same serialization setup, same mental model) and needs no
  separate converter library.
- **Networking failures never crash a screen.** `LeaderboardResult` is a
  small sealed `Success`/`Failure` type; screens show a retry state instead.
- **Rendering is 1:1 with game units.** `GameConfig`'s arena size is set
  directly from the measured Compose canvas size in pixels, so `engine`
  output never needs a coordinate transform before drawing.
- **Sound is `ToneGenerator` beeps**, not bundled audio files — deliberately
  the simplest thing that actually works, so the "sound effects" toggle in
  Settings isn't a dead switch.

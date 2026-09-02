# Privacy

Sneakster has no accounts, no login, and no analytics or crash-reporting
service. This document describes exactly what data the app and its backend
handle, based on what the code actually does — not a legal template.

If you self-host the backend (see `backend/README.md`) or publish your own
build of the app, you are the operator of that data, and should adapt this
document (and where you host it) to match your own setup before directing
anyone else to it.

## What the app collects

- **Nickname.** A name you type into Settings, stored only on your device
  until you submit a score or contribute to the shared effects pool — at
  that point it's sent to the backend and shown publicly on the leaderboard
  and/or as the "from" name on a pool gift. It's never validated against any
  real identity; you can pick anything permitted by the nickname rules
  (letters, numbers, spaces, `-`, `_`, 1–20 characters, and not one of a
  small list of blocked words — see `backend/README.md`).
- **Device ID.** A random identifier generated once on first launch and
  stored only on your device. It's sent to the backend only when
  contributing to the shared effects pool, solely so the server can limit
  how many contributions one install can make per hour. It isn't linked to
  a nickname, an account, or anything else, and nothing else is ever looked
  up by it.
- **IP address.** The backend's rate limiter uses your connection's IP
  address in memory to count requests per minute. It is never written to
  the database or any log file the app or backend produce, and is discarded
  as soon as the rate-limit window passes.

## What the app does not do

- No accounts, no passwords, no email addresses.
- No analytics, no advertising, no third-party tracking SDKs.
- No crash-reporting service — an unhandled crash is only logged locally to
  the device's own system log (logcat), never transmitted anywhere.
- No location data, contacts, camera, microphone, or storage access beyond
  what's needed to run the game.

## Data you can remove

Nickname and settings live in the app's local storage and are cleared by
uninstalling the app or clearing its data in Android's app settings.
Because there are no accounts, a leaderboard entry or pool contribution
already submitted to a backend can't be removed by the app itself — contact
whoever operates that backend if you want an entry removed.

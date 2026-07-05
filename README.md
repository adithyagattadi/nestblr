# NestBLR

NestBLR is an Android app for finding PG/coliving accommodation in Bengaluru directly from the
people who own it. PG hunting here is broker-heavy and full of stale or fake listings; this app
puts tenants in front of owner-managed listings they can search, filter, map, and call directly —
and gives owners the tools to list, photograph, and keep their rooms up to date. This repo is the
Android client; the Ktor backend lives in a separate repo (linked below).

## 🚀 Live

The app now points at production: **[nestblr-backend.onrender.com](https://nestblr-backend.onrender.com)**.
That means a fresh install works from any network — WiFi, mobile data, hotspot, anywhere. No LAN setup required.

## Screenshots



## Features

Tenant:
- Locality-based search across 12 Bengaluru localities (alphabetical picker)
- List and map views, the map rendered with OpenStreetMap tiles
- Filters: gender, food, PG type, rent range
- Detail screen with a photo carousel
- "Call owner" that logs the inquiry
- Favorites
- Reviews — star rating plus comment
- "Use my current location" via fused location

Owner:
- Full listing CRUD from a bottom-sheet menu
- Photo manager — upload (with client-side compression), delete, set cover
- Quick room availability edit
- Inquiries summary screen showing aggregated tenant activity

## Stack

- Kotlin 2.1.21
- Jetpack Compose (BOM 2026.05.00), Material 3
- Hilt 2.56 for dependency injection
- Retrofit 2.11.0
- Coil 2.7.0 for image loading
- osmdroid 6.1.20 for maps
- Firebase Auth
- Kotlin Coroutines
- Type-safe Compose Navigation
- Plus Jakarta Sans + Inter via downloadable Google Fonts

## Architecture

Clean Architecture split into `data/`, `domain/`, `feature/`, and `core/`. MVVM with `StateFlow`
exposed from ViewModels. Hilt provides dependencies throughout. A repository layer sits between the
Retrofit API and the feature layer. Bottom-sheet-scoped ViewModels use assisted injection so they
can take the listing/room id they operate on as a runtime parameter.

## Running locally

1. Place `google-services.json` at `app/`.
2. Build and install: `./gradlew :app:installDebug`

The `BASE_URL` in `app/build.gradle.kts` points at the deployed backend (`https://nestblr-backend.onrender.com/`). No local backend needed.

To point at a local backend for development (e.g. testing schema changes), edit `BASE_URL`:
- Emulator → host loopback: `http://10.0.2.2:8080/`
- Physical device on same WiFi as Mac: `http://<mac-lan-ip>:8080/`

If pointing at a plain-HTTP local backend, you'll also need to re-add `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`. Removed in production since Render provides HTTPS.

## What I Learned

OpenStreetMap as the no-billing alternative. I wanted Google Maps, but its billing-card requirement
clashed with the "free, no card" constraint I'd set myself. Switching to osmdroid + OSM tiles let me
ship a real map view at zero infrastructure cost. The same constraint bit again when I needed
geocoding: Nominatim's public server answers automated traffic with 403s, so I fell back to a
hardcoded list of 12 Bengaluru localities. The constraints quietly shaped the architecture, and I
think honestly.

State staleness in Compose bottom sheets. A sticky `saveComplete = true` flag caused reopened bottom
sheets to auto-dismiss themselves — the flag was still true from the previous time. The fix was a
one-shot `Channel<Unit>(CONFLATED)` for the dismissal signal, and moving `load()` out of `init` into
a `LaunchedEffect` so it re-runs each time the sheet opens. The bigger lesson was that
scope-retention semantics for ViewModels obtained via `hiltViewModel(key = X)` matter — they
survive composable disposal, so any state I leave set survives with them.

Diagnosing environment vs. code. A photo-upload "bug" turned out to be `BitmapFactory.decodeStream`
returning null in `inJustDecodeBounds = true` mode — which is its defined behavior, but a naive elvis
operator read it as failure. Separately, backend timestamps came back as
`2026-06-13 07:45:55.403238+05:30` (space separator, explicit offset), not the ISO `T...Z` I'd
assumed, and `Instant.parse` crashes on that; `OffsetDateTime.parse` after normalizing the space
handles both. Both taught the same instinct: when "it doesn't work," print the actual value and
verify before fixing.

## What's not built

A fair amount is deferred or wouldn't survive production scrutiny — no tests, no ProGuard rules, no
crash reporting, no CI, and more. See [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md).

## Related repo

Backend: [adithyagattadi/nestblr-backend](https://github.com/adithyagattadi/nestblr-backend).

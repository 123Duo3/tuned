# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: fresh skeleton, greenfield

The previous 2023 version is archived on the `legacy-2023` branch. `main` holds the step-1 skeleton (architecture package structure `core/ data/ domain/ presentation/ ui/ navigation/ di/`, Koin wired via `TunedApplication` + empty `appModule`, the type-safe `Route` graph rendering placeholders, `core/` Outcome/AppError types, a `domain/player/PlaybackController` interface) plus the start of **step 2**: the Room identity model (Podcast/Episode/Progress entities, DAOs, `TunedDatabase` with checked-in schema v1, and a tested `FeedIdentity` helper) and a SAX `RssFeedParser` with fixture tests. No repositories, ViewModels, feature logic, or Media3 implementation exist yet. Build forward following the stack and architecture defined here.

Current pinned versions (in `gradle/libs.versions.toml`): **AGP 9.2.1, Kotlin 2.3.21, Compose BOM 2026.05.01, compileSdk 36.1, minSdk 26**. `minSdk 26` is intentional: variable font support is a product-level visual requirement. Do not lower it for compatibility metrics. Source lives under `app/src/main/kotlin/ink/duo3/tuned/` (renamed from the template's default `java/` dir; AGP auto-registers `src/main/kotlin` with no extra `sourceSets` config). `AGENTS.md` is a symlink to this file — edit only `CLAUDE.md`.

**Catalog & tooling status** — the full agreed stack is declared in `gradle/libs.versions.toml`. **Wired into `app/build.gradle.kts`:** Koin, Navigation + kotlinx.serialization, lifecycle/Compose, KSP + Room (with `room { schemaDirectory(...) }` schema export to `app/schemas/`), Konsist (test). **Declared but not yet wired** (added when their build-order step lands): Media3, Ktor, Coil 3, WorkManager, DataStore. Quality gates are live: ktlint, detekt (`config/detekt/detekt.yml`), Konsist boundary tests (`ArchitectureBoundaryTest`), Dependabot, and GitHub Actions CI (`.github/workflows/ci.yml`).

## Product thesis (read this first — it drives every scope call)

Tuned is **a modern, libre Android podcast client** — filling the vacuum left after Google Podcasts shut down. The differentiator is **craftsmanship**, not features: AntennaPod is free but dated; Pocket Casts is modern but closed and (for the author) has a blocking bug. Tuned aims at the intersection: **modern Material 3 UI × no lock-in × actually reliable on Android.**

- **Android-only is a principled decision**, not a temporary scope cut. Do not propose KMP or iOS. Apple Podcasts owns iOS; we don't compete there.
- **"Libre" means functional freedom, not a license stance**: subscribe to *any* RSS, OPML in/out, never lock the user to a content platform. It does NOT mandate FOSS license, a no-tracking crusade, or free-of-charge. Closed-source deps are allowed.
- **Discovery is table-stakes, not the point.** The home feed is just Apple/iTunes Top Charts. Don't over-invest in recommendation/editorial features.
- **Reliability is a first-class feature.** The author left Pocket Casts over a malignant bug. The playback layer especially must not drop progress, die in the background, or mangle notifications.

## Tech stack

- **Kotlin 2.1+** (Compose Compiler is the Kotlin plugin — no separate compiler version)
- **Jetpack Compose + Material 3** (Expressive)
- **Navigation 3** with type-safe (`@Serializable`) `NavKey` routes — no string routes
- **Media3** (ExoPlayer + `MediaSessionService`) for playback. Upgrade to `MediaLibraryService` only if v1 exposes a browsable library to external clients such as Android Auto.
- **Ktor Client** + kotlinx.serialization (chosen over Retrofit)
- **Room** (KMP-capable) + DataStore
- **Koin** for DI — deliberately chosen over Hilt for faster builds and less boilerplate at this app's scale. Do not reintroduce Hilt.
- **WorkManager** for background feed refresh; **Coil 3** for images
- Quality gates: **ktlint + detekt + Konsist** (architecture tests) + Dependabot
- **JDK 17 toolchain** — keep Gradle, IDE, and CI builds on the same toolchain

## Architecture — single Gradle module, packages as future modules

One `:app` module, organized so promoting to multi-module later is mechanical. Package layout under `ink.duo3.tuned`:

```
core/          common utils, Result/AppError types
data/          network/ (Ktor API clients), local/ (Room), model/ (DTOs), repository/ (impls)
domain/        model/ (pure-Kotlin domain types), repository/ (interfaces), player/ (PlaybackController + playback models)
player/media3/ Media3 implementation + service — the ONLY package importing androidx.media3.*
presentation/  home/, library/, search/, episode/, player/ — presentation logic only: ViewModel + UiState
ui/            home/, library/, search/, … (Compose screens) + components/ (shared components) + theme/
navigation/    type-safe routes + the central NavDisplay
di/            Koin modules; the composition root that wires implementations to interfaces
```

Each screen is a **"page"** split across two roots: its `presentation/<name>` holds the ViewModel + UiState; its `ui/<name>` holds the Compose screen. The `ui/<name>` screen is the only thing that imports its matching `presentation/<name>` ViewModel.

**Four boundary rules — enforced by Konsist tests in CI. Treat a violation as a build failure:**

1. Pages never import each other (neither `presentation/<a>` nor `ui/<a>` may reach another page's `presentation`/`ui`). Cross-page navigation goes through Navigation routes, not code references.
2. For project-internal imports, a page may only reach `domain`, `core`, `navigation`, the shared UI packages (`ui/components`, `ui/theme`), and its own page — never `data.*`, Room, Ktor, or Media3 directly. AndroidX UI, lifecycle, Coil, etc. are unconstrained.
3. `androidx.media3.*` appears **only** in `player/media3/`. The rest of the app talks to `domain/player/PlaybackController`.
4. `data/repository/` classes implement interfaces declared in `domain/repository/`. UI/ViewModels depend on the interface, never the impl — so swapping a data source (e.g. iTunes → Podcast Index) never touches UI.

Additional conventions:
- A ViewModel exposes exactly one `StateFlow<XxxUiState>` plus event functions. Don't make the UI `collect` multiple flows.
- No UseCase layer until logic genuinely spans multiple repositories — ViewModels calling repositories directly is fine and preferred.
- DataStore is for preferences only (theme, country, playback speed, refresh policy). Business data belongs in Room.
- Wrap experimental Material 3 Expressive APIs behind `ui/components`. Do not scatter preview APIs across screens.
- Split a file before it crosses ~300 lines.

## MVP scope (v1.0)

Three subscription entry points, all platform-independent (this triad *is* the "no lock-in" promise):
- **Top Charts → subscribe** (replaceable charts adapter, by country + category) — the home feed
- **Search → subscribe** (iTunes Search API; the search box also accepts a raw RSS URL and auto-detects)
- **OPML import + export** — the migration path in and the guarantee that Tuned never locks users in

Plus: podcast detail (RSS episode list) · episode detail (HTML show notes) · **Media3 player** (background, notification, lockscreen, 1.5x/2x, ±15s/±30s) · resume playback (position stored in Room) · subscription persistence + library · daily WorkManager refresh · sleep timer.

**Deferred (post-v1):** downloads/offline, play queue, new-episode notifications, chapters, transcripts, listening stats.

Start with stable identity and synchronization fields. Do not reserve speculative columns merely to avoid Room migrations:

```
Podcast: id, canonicalFeedUrl, currentFeedUrl, etag, lastModified, lastFetchedAt
Episode: id, podcastId, guid, enclosureUrl, publishedAt, durationMs
Progress: episodeId, positionMs, completed, lastPlayedAt
```

Define and test the fallback identity rule for episodes that omit `guid`. Treat feed redirects, duplicate episodes, malformed individual items, and one-feed refresh failures as expected inputs, not exceptional surprises.

## Data sources

- **iTunes Search** — free, no auth, best Chinese-podcast coverage. Primary search source for MVP. Responses include the RSS `feedUrl`, which feeds the RSS pipeline. The API is rate-limited, so debounce and cache search requests.
- **Top Charts adapter** — keep discovery behind a replaceable interface. Validate the real Apple Charts endpoint before treating it as stable; do not couple UI or domain models to its wire format.
- **Podcast Index** — open, free (API key), good for trending/Podcasting-2.0; weaker CN coverage. Candidate for later.
- Chinese walled gardens (小宇宙 / 喜马拉雅 / 荔枝) have **no public API** and are not RSS-based — not usable as sources.

## Recommended build order

1. Project skeleton + CI (package structure, Koin init, Konsist rules, Dependabot, ktlint/detekt, Room schema export).
2. Room identity model + RSS URL import with fixture tests for redirects, missing GUIDs, duplicates, and malformed items.
3. One vertical playback loop: import one RSS URL → show episodes → play one episode through the Media3 service → persist progress → resume after service/process recreation.
4. Complete reliability work for playback, then add iTunes search, OPML import/export, library, and WorkManager refresh.
5. Add discovery last: Top Charts adapter → home.

## Reliability acceptance criteria

Playback is not done merely because audio starts. Before v1, verify pause/resume and progress persistence across backgrounding, task removal, service recreation, and process death. Verify notification, lockscreen, headset, Bluetooth, audio-focus, network-loss, and reconnect behavior on real devices.

Persist progress periodically with throttling and flush it on pause, stop, episode transition, and service teardown. Decide explicitly whether `MediaSessionService` is sufficient or whether `MediaLibraryService` is required for a browsable Android Auto/Wear OS library. If using `MediaLibraryService`, define the browse tree and external-controller policy.

Feed refresh is isolated per feed: one broken feed must not fail the whole run. Use bounded concurrency, network constraints, retry/backoff, and conditional requests with `ETag` / `Last-Modified`.

## Test and dependency policy

- Keep Room schema JSON files in version control and add migration tests from the first schema change.
- CI runs `assembleDebug`, `assembleRelease`, `test`, `lint`, `ktlintCheck`, and `detekt`.
- Add RSS parser fixture tests before relying on live feeds in feature tests.
- Dependabot may propose updates, but never auto-merge alpha or beta dependencies.

## Commit convention

**Conventional Commits with a scope**, imperative mood, no trailing period:

```
<type>(<scope>): <subject>
```

- **Types:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`, `ci`, `chore`.
- **Scope = the package/area touched:** `core`, `data`, `domain`, `player`, `discover`, `library`, `search`, `episode`, `di`, plus infra scopes `build` (Gradle/version catalog), `ci`, `deps`. Omit the scope only for genuinely repo-wide changes.
- **Subject line only — no body/description.** Keep the subject under ~70 chars and self-explanatory; do not add an explanatory body.
- **Do NOT add a `Co-Authored-By` trailer** (or any AI-attribution footer), even for agent-authored commits — the author opted out.

Examples: `feat(player): add sleep timer`, `fix(rss): handle feeds with no <image>`, `build(deps): add media3 + koin to catalog`, `refactor(domain): extract PlayerController interface`.

## Common commands

Standard Android Gradle (the skeleton builds today; ktlint/detekt/Konsist gates are configured and live):

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on a connected device/emulator
./gradlew test                   # JVM unit tests (all)
./gradlew testDebugUnitTest --tests "ink.duo3.tuned.SomeClassTest"   # a single test class
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
./gradlew lint                   # Android Lint
./gradlew ktlintCheck detekt     # style + static analysis
```

Konsist architecture tests run as part of `test`; a boundary violation fails the build.

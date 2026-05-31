# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⚠️ Status: fresh skeleton, greenfield

The previous 2023 version (and its git history) has been **wiped**. The repo is now a **stock Android Studio "Empty Compose Activity" template** on a single `Initialize empty main branch` commit — just `MainActivity.kt` + `ui/theme/`. There is **no app logic yet**; everything in the architecture/MVP sections below still needs to be built. Build from this skeleton following the stack and architecture defined here.

Current pinned versions (in `gradle/libs.versions.toml`): **AGP 9.3.0-alpha06, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk 36, minSdk 26**. Note AGP is an **alpha** — expect occasional toolchain churn. Source lives under `app/src/main/kotlin/ink/duo3/tuned/` (renamed from the template's default `java/` dir; AGP auto-registers `src/main/kotlin` with no extra `sourceSets` config). `AGENTS.md` is a symlink to this file — edit only `CLAUDE.md`.

**Not yet added** — the agreed stack deps (Media3, Ktor, Room, Koin, Coil 3, Navigation, kotlinx.serialization, WorkManager) and quality tooling (ktlint, detekt, Konsist) are **not in the catalog or build files yet**. Adding them to `libs.versions.toml` is step 1 of the build order below.

## Product thesis (read this first — it drives every scope call)

Tuned is **a modern, libre Android podcast client** — filling the vacuum left after Google Podcasts shut down. The differentiator is **craftsmanship**, not features: AntennaPod is free but dated; Pocket Casts is modern but closed and (for the author) has a blocking bug. Tuned aims at the intersection: **modern Material 3 UI × no lock-in × actually reliable on Android.**

- **Android-only is a principled decision**, not a temporary scope cut. Do not propose KMP or iOS. Apple Podcasts owns iOS; we don't compete there.
- **"Libre" means functional freedom, not a license stance**: subscribe to *any* RSS, OPML in/out, never lock the user to a content platform. It does NOT mandate FOSS license, a no-tracking crusade, or free-of-charge. Closed-source deps are allowed.
- **Discovery is table-stakes, not the point.** The home feed is just Apple/iTunes Top Charts. Don't over-invest in recommendation/editorial features.
- **Reliability is a first-class feature.** The author left Pocket Casts over a malignant bug. The playback layer especially must not drop progress, die in the background, or mangle notifications.

## Tech stack

- **Kotlin 2.1+** (Compose Compiler is the Kotlin plugin — no separate compiler version)
- **Jetpack Compose + Material 3** (Expressive)
- **Compose Navigation 2.8+** with type-safe (`@Serializable`) routes — no string routes
- **Media3** (ExoPlayer + `MediaLibraryService`) for playback
- **Ktor Client** + kotlinx.serialization (chosen over Retrofit)
- **Room** (KMP-capable) + DataStore
- **Koin** for DI — deliberately chosen over Hilt for faster builds and less boilerplate at this app's scale. Do not reintroduce Hilt.
- **WorkManager** for background feed refresh; **Coil 3** for images
- Quality gates: **ktlint + detekt + Konsist** (architecture tests) + Dependabot

## Architecture — single Gradle module, packages as future modules

One `:app` module, organized so promoting to multi-module later is mechanical. Package layout under `ink.duo3.tuned`:

```
core/        design system, common utils, Result/AppError types
data/        network/ (Ktor API clients), local/ (Room), model/ (DTOs), repository/ (impls)
domain/      model/ (pure-Kotlin domain types), repository/ (interfaces) — no Android/Room/Ktor/Media3
player/      Media3 service + a PlayerController interface — the ONLY package importing androidx.media3.*
feature/     discover/, library/, search/, episode/, player/ (UI) — each = screen + ViewModel + UiState
di/          Koin modules
```

**Four boundary rules — enforced by Konsist tests in CI. Treat a violation as a build failure:**

1. `feature/*` packages never import each other. Cross-feature navigation goes through Navigation routes, not code references.
2. `feature/*` may only import `domain` and `core` — never `data.*`, Room, Ktor, or Media3 directly.
3. `androidx.media3.*` appears **only** in `player/`. The rest of the app talks to a `PlayerController` interface.
4. `data/repository/` classes implement interfaces declared in `domain/repository/`. UI/ViewModels depend on the interface, never the impl — so swapping a data source (e.g. iTunes → Podcast Index) never touches UI.

Additional conventions:
- A ViewModel exposes exactly one `StateFlow<XxxUiState>` plus event functions. Don't make the UI `collect` multiple flows.
- No UseCase layer until logic genuinely spans multiple repositories — ViewModels calling repositories directly is fine and preferred.
- Split a file before it crosses ~300 lines.

## MVP scope (v1.0)

Three subscription entry points, all platform-independent (this triad *is* the "no lock-in" promise):
- **Top Charts → subscribe** (iTunes Top Charts, by country + category) — the home feed
- **Search → subscribe** (iTunes Search API; the search box also accepts a raw RSS URL and auto-detects)
- **OPML import** — the migration path for Pocket Casts / Google Podcasts refugees; high priority

Plus: podcast detail (RSS episode list) · episode detail (HTML show notes) · **Media3 player** (background, notification, lockscreen, 1.5x/2x, ±15s/±30s) · resume playback (position stored in Room) · subscription persistence + library · daily WorkManager refresh · sleep timer.

**Deferred (post-v1):** downloads/offline, play queue, new-episode notifications, OPML *export*, chapters, transcripts, listening stats.

**Reserve these `Episode` fields from day 1** to avoid future migrations: `chapters` (Podcasting 2.0), `transcriptUrl`, `playbackPositionMs`, `lastPlayedAt`, `isDownloaded`, `localPath`.

## Data sources

- **iTunes Search / Top Charts** — free, no auth, best Chinese-podcast coverage. Primary source for MVP. Responses include the RSS `feedUrl`, which feeds the RSS pipeline.
- **Podcast Index** — open, free (API key), good for trending/Podcasting-2.0; weaker CN coverage. Candidate for later.
- Chinese walled gardens (小宇宙 / 喜马拉雅 / 荔枝) have **no public API** and are not RSS-based — not usable as sources.

## Recommended build order

1. Project skeleton + CI (package structure, Koin init, Konsist rules, Dependabot, ktlint/detekt). This locks in maintainability.
2. The `player/` package first (Media3 service + `PlayerController`) — the hardest and most error-prone part; build it with no feature pressure.
3. Data layer + Room schema + iTunes/RSS data sources.
4. Features, simplest-first: search → podcast detail → home → library.

## Commit convention

**Conventional Commits with a scope**, imperative mood, no trailing period:

```
<type>(<scope>): <subject>
```

- **Types:** `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`, `ci`, `chore`.
- **Scope = the package/area touched:** `core`, `data`, `domain`, `player`, `discover`, `library`, `search`, `episode`, `di`, plus infra scopes `build` (Gradle/version catalog), `ci`, `deps`. Omit the scope only for genuinely repo-wide changes.
- Keep the subject under ~70 chars; put the "why" in the body when it isn't obvious.
- **Do NOT add a `Co-Authored-By` trailer** (or any AI-attribution footer), even for agent-authored commits — the author opted out.

Examples: `feat(player): add sleep timer`, `fix(rss): handle feeds with no <image>`, `build(deps): add media3 + koin to catalog`, `refactor(domain): extract PlayerController interface`.

## Common commands

Standard Android Gradle (the skeleton builds today; ktlint/detekt/Konsist targets work only after they're configured):

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on a connected device/emulator
./gradlew test                   # JVM unit tests (all)
./gradlew testDebugUnitTest --tests "ink.duo3.tuned.SomeClassTest"   # a single test class
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
./gradlew lint                   # Android Lint
./gradlew ktlintCheck detekt     # style + static analysis (once configured)
```

Konsist architecture tests run as part of `test`; a boundary violation fails the build.

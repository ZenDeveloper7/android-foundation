# Android Foundation

A starting point for new Android apps, with build, test and lint gates: modular, single-activity
Jetpack Compose, following Google's [app architecture guidance](https://developer.android.com/topic/architecture).
A small notes app with settings demonstrates every layer end to end; replace it with your features.
It has not yet been validated on devices (see [docs/TESTING.md](docs/TESTING.md)).

## Stack

| Area | Choice |
| --- | --- |
| Language and UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09.00), Material 3 with adaptive navigation |
| Architecture | MVVM with unidirectional data flow, `StateFlow` UI state, repository interfaces |
| Dependency injection | Hilt (KSP), assisted injection for ViewModels with arguments |
| Navigation | Navigation 3, single activity, back stack saved across process death |
| Data | Room (per-row suspend writes, `Flow` reads, exported schemas), Preferences DataStore |
| Network | Retrofit 3, OkHttp 5, kotlinx.serialization (HTTPS only) |
| Build | AGP 9.4, Gradle 9.8, version catalog, `build-logic` convention plugins |
| Quality | Lint (warnings are errors), Kotlin warnings as errors in CI, ktlint + Compose rules via Spotless |
| Tests | JUnit, Truth, Turbine, coroutines-test, Robolectric Compose UI tests, MockWebServer |
| Performance | Baseline Profiles, startup Macrobenchmark, StrictMode in debug |
| SDK levels | min 26, target and compile 37 |

## Quick start

Requirements: JDK 25 (the Gradle daemon is pinned to 25 in `gradle/gradle-daemon-jvm.properties`;
CI uses the same; bytecode still targets Java 17), Android SDK platform 37, Python 3.

```bash
./gradlew :app:installDebug
```

```bash
scripts/verify.sh
```

`scripts/verify.sh` is the definition of done. It runs the repository policy checks
(`scripts/check_policies.py`), formatting, lint for every module, all unit and UI tests, and debug
and minified release builds. CI (`.github/workflows/ci.yml`) runs the same
script on every pull request.

To start a new app from this template, follow [docs/NEW_APP.md](docs/NEW_APP.md). To move an
existing app onto it, follow [docs/MIGRATE_EXISTING_APP.md](docs/MIGRATE_EXISTING_APP.md).

## Modules

```
app                     Application, MainActivity, navigation shell
feature/notes           Notes list and editor (sample feature)
feature/settings        Theme and dynamic color (sample feature)
core/data               Repository interfaces + implementations
core/database           Room
core/datastore          Preferences DataStore
core/network            OkHttp + Retrofit + JSON
core/model              Plain Kotlin models
core/common             Dispatchers, scopes, Clock, helpers
core/designsystem       Theme, tokens, components, previews
core/testing            Fakes and test rules
benchmark               Baseline Profile + startup benchmark
build-logic             Convention plugins
```

## Secure defaults

- Only the launcher activity is exported.
- Cleartext HTTP is blocked; only system CAs are trusted (user CAs in debug builds, for proxies).
- `INTERNET` is declared by `:core:network` only, so removing that module removes the permission.
- Backup is an explicit allowlist of files.
- Release builds are signed only from environment variables and never fall back to the debug key.
- Network logging is debug-only and redacts credentials.

## Documentation

- [Architecture](docs/ARCHITECTURE.md): layers, module rules, navigation, state, persistence
- [Conventions](docs/CONVENTIONS.md): the review checklist, with links to Google guidance
- [Testing](docs/TESTING.md): what to test where, and how
- [Starting a new app](docs/NEW_APP.md): rename, rebrand, add features and data
- [Migrating an existing app](docs/MIGRATE_EXISTING_APP.md): phased playbook and the `scripts/migration/` tools
- [Releasing](docs/RELEASE.md): signing, Baseline Profiles, release checklist
- [AGENTS.md](AGENTS.md): instructions for coding agents

## Not included (add when an app needs it)

Authentication, a real backend, analytics or crash reporting, push notifications, WorkManager,
screenshot tests, list-detail layouts for large screens, and localized strings beyond English.

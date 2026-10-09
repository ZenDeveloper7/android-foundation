# Android Foundation

[![Android quality](https://github.com/ZenDeveloper7/android-foundation/actions/workflows/android.yml/badge.svg)](https://github.com/ZenDeveloper7/android-foundation/actions/workflows/android.yml)

A runnable Kotlin/Compose starter and executable coding standards for future Android apps.
Built from scratch; Quick Search informed the review lessons, but its application source was not copied.

The sample task app demonstrates persistent create/read/update/delete, context-free ViewModels,
constructor injection, observable Room data, cancellation-safe writes, saved drafts, saved navigation,
failure/retry UI, destructive-action confirmation, and phone/tablet/large-text configurations.

## Start here

1. Use GitHub's **Use this template** button to create your own repository.
2. Open the checkout in Android Studio with support for AGP 9.4 and SDK 37.
3. Install JDK 21, Android SDK platform 37 and Python 3. Java/Kotlin bytecode targets 17.
4. Run `./gradlew :app:assembleDebug`, then install `app/build/outputs/apk/debug/app-debug.apk`.
5. Read [coding standards](docs/CODING_STANDARDS.md), [architecture](docs/ARCHITECTURE.md),
   [testing](docs/TESTING.md), and [starting a new app](docs/NEW_APP.md).

```sh
git clone https://github.com/ZenDeveloper7/android-foundation.git
cd android-foundation
./gradlew :app:assembleDebug
./gradlew verify
```

Set `ANDROID_HOME` to your SDK directory or configure an ignored `local.properties`.
On Windows use `gradlew.bat`; Python 3 must be available as `python` (other platforms: `python3`).

## Modules

| Module | Responsibility |
| --- | --- |
| `app` | Application composition, Hilt entry point and Navigation 3 destinations |
| `core:data` | Repository contract/models, Room schema, per-record suspend writes and DI bindings |
| `core:designsystem` | Shared Material 3 theme; no dependency on data or feature code |
| `feature:tasks` | Screen state, ViewModel and accessible stateless Compose UI |

The initial project intentionally has no authentication, backend, analytics or product SDKs.
Add them behind feature-specific contracts when the consuming app needs them; see the extension guidance.

## Quality gates

`./gradlew verify` runs architecture/security policy checks, Spotless formatting, lint with warnings as
errors for every module, ViewModel tests, SQLite/Room tests, Compose behavior/configuration tests,
navigation serialization tests, debug assembly and a minified **unsigned** release build.
The GitHub Actions workflow runs the same gate and retains reports/APKs. Dependency update PRs are
configured with Dependabot; branch protection must be enabled in the consuming repository.

Formatting: `./gradlew spotlessApply`. Never update test assertions or suppress lint just to make a
failed gate green; investigate the behavior. [Validation status](docs/VALIDATION.md) distinguishes local
checks, remote CI and device evidence.

## Secure defaults

Only the launcher activity is exported and it ignores external payloads. No permissions are requested.
Cleartext traffic is disabled. App data is excluded from cloud backup and device transfer by explicit
file-level rules; the consuming app must define and test its own restore policy. Release artifacts
are unsigned and never fall back to the debug certificate. No secrets, signing keys or local paths are
part of this repository.

This is an initial reference foundation, not a certification of every future app built from it.
Runtime TalkBack, OEM backup/transfer behavior, process termination, folding, IME/system bars and
performance need device validation for the actual product.

## Toolchain

Versions are pinned in `gradle/libs.versions.toml`, not described as perpetually "latest".
Initial baseline: AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21, Compose BOM 2026.09.00,
Navigation 3 1.2.0, Lifecycle 2.11.0, Room 2.8.5, Hilt 2.60.1; compile/target SDK 37, minimum 24.

## License

Original code is MIT licensed. Gradle wrapper files retain their upstream Apache 2.0 license notices;
see [NOTICE](NOTICE) and [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt).

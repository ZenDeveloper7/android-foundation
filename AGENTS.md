# Agent guide

Playbook for coding agents (Claude Code, Codex and others). When this file and the code disagree,
trust the code and build files.

## Project

- Android foundation template: Kotlin, Jetpack Compose, Material 3, Hilt, Navigation 3, Room,
  DataStore, Retrofit. Single activity. Package `com.zen.foundation` (renamed per app).
- Read `docs/ARCHITECTURE.md` and `docs/CONVENTIONS.md` before changing structure or adding code.
- Module setup lives in `build-logic/convention`; versions only in `gradle/libs.versions.toml`.

## Working rules

- Keep changes narrow. Don't mix feature work with refactors. Preserve unrelated work in the tree.
- Don't commit, push, tag or publish without explicit permission.
- Ask before adding a dependency, Gradle plugin or module.
- Never change signing configuration, add a keystore, or make release builds use the debug key.
- Features must not depend on other features; reach data only through `:core:data` interfaces.
- All user-facing text goes in module-prefixed string resources.
- Never weaken a check to get a green build: no lint disables, no test assertion edits, no
  `@Suppress` without a comment explaining why.

## Validation

- Definition of done: `scripts/verify.sh` prints `VERIFY PASSED`. Use `--quick` while iterating.
  It runs the policy checks (`scripts/check_policies.py`) before Gradle.
- JDK 25 (matches `gradle/gradle-daemon-jvm.properties`; bytecode still targets Java 17).
- Single module: `./gradlew :feature:notes:testDebugUnitTest`, `./gradlew :feature:notes:lintDebug`.
- Formatting: `./gradlew spotlessApply` (only touches files that need it).
- A passing build proves compilation and tests only. Don't claim visual, gesture, accessibility,
  backup or performance behavior without checking it on a device, and say which device.
- Report validation precisely: not run, static only, compiled, unit-tested, device-tested.

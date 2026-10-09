# Validation status

Initial validation: **9 October 2026**.

## Local gate

`./gradlew spotlessApply verify` passed with the pinned toolchain: Gradle 9.8.1, AGP 9.4.1,
Kotlin 2.4.21, compile/target SDK 37. Local runtime was OpenJDK 25; CI uses Temurin 21.
The local Kotlin compiler was run in-process. Both runtimes target Java/Kotlin bytecode 17.

| Check | Result |
| --- | --- |
| Architecture/source/manifest policy checks | Passed |
| Spotless/ktfmt | Passed |
| Android lint, all four modules | Zero findings |
| ViewModel tests | 8 passed |
| Compose interaction/configuration/restoration tests | 4 passed |
| Room/SQLite persistence tests | 3 passed |
| Navigation/back/restoration tests | 2 passed |
| Navigation-key serialization | 1 passed |
| Debug APK | Built |
| Minified release APK | Built unsigned |

Total: **18 tests, 0 failures**. Dependency-version notification rules are intentionally delegated
to reviewed Dependabot updates, as documented in `lint.xml`; source/resource/compatibility checks
remain enabled. No lint baseline hides existing source issues.

The Gradle run emits a Gradle-10 deprecation notice and native-access warnings on the local JDK 25.
These do not fail the pinned build. Re-evaluate plugin compatibility before a Gradle major upgrade.

## Remote gate

The public workflow is [Android quality](https://github.com/ZenDeveloper7/android-foundation/actions/workflows/android.yml).
Its badge and run history show the current commit's remote result; a local pass is not substituted for
that result. CI runs the same verification task and retains test/lint reports and APKs.

## Runtime limits

No physical-device or emulator session was driven during initial implementation. Compose behavior
and Room SQLite tests ran locally through Robolectric API 35. Size/font overrides and saveable-state
restoration are simulations, not screenshot diffs, actual process-kill proof or OEM behavior evidence.

Before publishing a consuming app, complete the device checklist in [TESTING.md](TESTING.md), including
TalkBack, keyboard/IME, system bars, resize/folding, process restart and the product's backup/restore
policy. Pixel baselines and measured macrobenchmark/baseline-profile journeys remain product work.

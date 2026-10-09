# Testing

Run `./gradlew verify` for the same gate used by CI. Use `./gradlew spotlessApply` before committing.

| Suite | Purpose | Command |
| --- | --- | --- |
| ViewModel | Observation, failure/retry, late saves, duplicate submission, restored input and IDs | `./gradlew :feature:tasks:testDebugUnitTest` |
| Room/SQLite | Concurrent insert preservation, targeted mutation/deletion and validation | `./gradlew :core:data:testDebugUnitTest` |
| Compose | Confirmation, saved dialog, empty state, phone/tablet/large font | Included in feature tests through Robolectric |
| Navigation | Serializable destination keys and saved/back navigation | `./gradlew :app:testDebugUnitTest` |
| Static policy | Initial module graph, blocked main-thread patterns, exports and signing | `python3 scripts/check_architecture.py` |
| Lint | Android and Compose platform checks with warnings as errors | `./gradlew lintDebug` |
| Release | Minified unsigned APK; no debug signing fallback | `./gradlew :app:assembleRelease` |

Tests use JUnit4, coroutine TestDispatcher, repository fakes and Robolectric API 35. Compose state
restoration is simulated by StateRestorationTester; SavedStateHandle restoration is independently
tested. Neither proves recovery after an actual OS process kill. Room tests exercise SQLite through
Robolectric; add/run device migration and persistence tests when deploying real user data.

Phone/tablet/large-font tests assert interaction/layout visibility; they are not screenshot-diff tests.
Previews are provided, but pixel baselines and a screenshot-review workflow must be added for the
consuming app's finalized design. Do not regenerate golden images merely to hide a regression.

Before release, validate on devices/emulators: app launch and back; add/complete/delete; persistence
after restart; input restoration after rotation/process death; IME, system bars, resize/folding;
TalkBack/keyboard/RTL/font scale; backup/transfer policy. Add macrobenchmarks and baseline profiles
for meaningful journeys, record hardware/compilation mode and compare measurements.

Coverage should focus on business/state behavior. No arbitrary coverage percentage is imposed in
this initial starter; generated Hilt/Room code should not inflate any future coverage report.
Reports live under each module's `build/reports/tests/` and `build/reports/lint-results-debug.html`.
GitHub CI uploads test/lint reports and build artifacts even when a gate fails.

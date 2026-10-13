# Testing

Follows [Android testing fundamentals](https://developer.android.com/training/testing/fundamentals).
Most tests are local (JVM) so they run on every change, in seconds, without a device.

| What | Where | How |
| --- | --- | --- |
| ViewModels | `feature/*/src/test` | Fakes from `:core:testing`, `MainDispatcherRule`, `runTest` |
| Repositories | `core/data/src/test` | Hand-written fake DAO, fixed `Clock` |
| Room DAOs | `core/database/src/test` | In-memory Room on Robolectric |
| DataStore | `core/datastore/src/test` | Real DataStore in a temporary folder |
| Network setup | `core/network/src/test` | `MockWebServer` |
| Compose screens | `feature/*/src/test` | `createComposeRule()` on Robolectric, against the stateless `XScreen` |
| Navigation rules | `app/src/test` | Pure functions on a `MutableList<NavKey>` |
| State restoration (dialogs, Navigation 3 back stack) | `app/src/test`, `feature/*/src/test` | `StateRestorationTester(composeRule)` simulates recreation |
| Configuration (font scale, size) | `feature/*/src/test` | Robolectric `@Config(qualifiers = ...)` plus `LocalDensity` with `fontScale = 2f` |
| Load, write and in-flight save errors | `feature/*/src/test` | `FakeNotesRepository`: `failReads`, `failWrites`, `writeGate` |
| Startup performance | `:benchmark` | Macrobenchmark on a device (manual) |

## Rules

- Prefer fakes over mocks. Shared fakes live in `:core:testing`; add one when you add a repository.
- Test `XScreen` with plain state. Don't start Hilt or a ViewModel in UI tests.
- To collect a `WhileSubscribed` `StateFlow` in a test, start a collector in `backgroundScope`
  with `UnconfinedTestDispatcher(testScheduler)` and then assert on `.value`.
- Never edit an assertion or add a suppression just to make a failing test pass. Find out why it
  fails first.
- Robolectric runs SDK 36 (`src/test/resources/robolectric.properties`) until it supports the
  compile SDK.

## Running

```bash
scripts/verify.sh --quick                              # formatting + all unit tests
./gradlew :feature:notes:testDebugUnitTest             # one module
./gradlew :feature:notes:testDebugUnitTest --tests '*NoteEditor*'
```

## Not yet included

- Screenshot tests (Roborazzi or Compose Preview Screenshot Testing) for design-system components.
- Instrumented Hilt tests (`@HiltAndroidTest`) for flows that need the real dependency graph.
- Room migration tests: add them with the first schema change (`room-testing` is already wired).

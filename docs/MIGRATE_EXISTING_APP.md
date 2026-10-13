# Migrating an existing app to the foundation

How to move an existing Android app onto this template's architecture without losing features.
Everything you copy comes from **this repository**:
- the convention plugins in `build-logic/`;
- `core:common`, including `UiText` and `EventQueue`;
- the verify and policy scripts;
- the config-driven migration scripts in `scripts/migration/`.

The target is described in [ARCHITECTURE.md](ARCHITECTURE.md) and [CONVENTIONS.md](CONVENTIONS.md).
For a brand-new app, use [NEW_APP.md](NEW_APP.md) instead.

The steps come from migrating a production app: one Gradle module with a very large ViewModel
and manual singletons, which became a Hilt app with a dozen modules. It appears below only as
"the pilot migration".

## Principles

- **Behavior first.** Every phase keeps the app doing exactly what it did. Restructuring and
  behavior changes never share a commit. Anything a user could notice goes into the target
  repo's `docs/MIGRATION.md` (see [The record for testers](#the-record-for-testers)).
- **Pin behavior before moving code** with characterization tests. They are the only proof the
  restructure kept features.
- **One phase, one green `scripts/verify.sh`, one local commit.** Never continue on red.
- **Scripts for mechanical work, people for judgment.** Scripts move files, rename packages and
  fix imports. A person decides how to split ViewModels and where module lines go.
- **Work on a new local branch and push nothing** until the owner has tested on a device.

## Running as an agent

When an agent (Codex, Claude Code or another) runs this playbook without a person watching:

- **The owner's decisions come from the prompt.** If a Phase 0 decision is missing, stop after
  the inventory and report. Don't pick a default.
- **The prompt names the phases for this run.** Stop after them and report:
  - what changed;
  - what `verify.sh` printed;
  - every visible difference;
  - what you need decided next.
- **This repository is read-only reference.** Copy from it; never edit it. Write the migration
  config and generated files next to the target project, not inside the template.
- **Gradle and `verify.sh` are allowed in the target repo**, because this playbook requires them.
  Devices, ADB, pushes, remote changes and signing changes are not, unless the prompt says so.
- **Stop and report instead of improvising when:**
  - a script doesn't fit the project (see [Script limits](#script-limits));
  - a collision or violation needs a design decision;
  - a characterization test fails;
  - a step would change behavior.

## Phase 0 — Assess and decide

Inventory the app:

| Area | What to record |
| --- | --- |
| Modules and flavors | Gradle modules, product flavors, build types, source sets |
| Entry points | Activities, services, receivers, deep links, `Application` |
| Navigation | Library, who owns the back stack, activity-per-flow screens |
| State | ViewModels (size, responsibilities), shared mutable singletons, `Context` in ViewModels |
| Dependency injection | Hilt, Koin, or manual `object` singletons |
| Data | Retrofit/OkHttp clients and interceptors, tokens/cookies, Room, DataStore/SharedPreferences |
| SDKs | Payments, video/RTC, analytics, crash reporting (keys, native libraries) |
| Secrets | Keys in code, keystores in git, signing configs |
| Quality | Tests, lint state, CI, R8 rules |
| Platform | min/target/compile SDK, AGP, Kotlin and Gradle versions |

Ask the owner for decisions that are theirs. Don't guess them:

1. Secrets in the repo (for example a tracked keystore): remove, ignore or keep for now.
2. minSdk: keep or raise. This template uses 26; an app may stay lower.
3. Feature modules: how many, named after which user-facing areas.
4. SDK ids and keys: move to `gradle.properties` / `local.properties`, or leave them.
5. Branch name, and whether anything may be pushed (default: no).
6. CI: add this template's workflow, keep the app's own, or none.

Record the decisions at the top of the target repo's `docs/MIGRATION.md`.

## Phase 1 — Safety net

1. Create the branch. Keep a snapshot of the original (a tag or a tarball) for comparisons.
2. Build and run the existing tests once to record the baseline. Fix nothing yet.
3. Write **characterization tests** for every ViewModel over about 300 lines and every flow
   involving money, auth or sessions. Assert what the code does today: state after each action,
   emitted events and navigation, even when it looks wrong. In the pilot migration, such tests
   pinned the old main ViewModel and caught regressions when it was split into four.
4. Tests that need resources run on Robolectric. This template's `KotlinAndroid.kt` already adds
   the JDK 21+ flags (`--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`,
   `--add-opens=java.base/java.io=ALL-UNNAMED`).

## Phase 2 — Build foundation

Copy from this repository into the app:

| Copy | Adapt |
| --- | --- |
| `build-logic/` | Rename plugin ids `foundation.*` to `<app>.*`. Drop `AndroidRoomConventionPlugin` without Room |
| `gradle/libs.versions.toml` | Merge. Keep the app's library versions where it pins them; add the plugin aliases |
| Root `build.gradle.kts`, `.editorconfig`, `lint.xml`, `gradle.properties` | Keep `org.gradle.jvmargs=-Xmx4g` (native SDKs packaged across modules ran out of memory at 2 GB) |
| `scripts/verify.sh`, `scripts/check_policies.py` | See "Staged verify" below |

**Staged verify.** The template's `verify.sh` assumes a finished migration: it runs
`spotlessCheck`, `-PwarningsAsErrors=true`, `:core:model` tasks and the benchmark compile. Start the
copied script with only what exists and passes today, then add each check in the phase that makes
it pass:

| Check | Add in |
| --- | --- |
| `check_policies.py`, `testDebugUnitTest`, `lintDebug`, `:app:assembleDebug`, `:app:assembleRelease` | Phase 2 |
| `:core:model:test` / `:core:model:lint` | Phase 5, if the app gets `core:model` |
| `-PwarningsAsErrors=true` (Kotlin compiler warnings) | After Phase 6, once warnings are fixed or the flag is scoped |
| `:benchmark:compileBenchmarkReleaseKotlin` | Phase 9 |
| `spotlessCheck` | Phase 9, the formatting commit |

**Policy findings in legacy code.** `check_policies.py` bans patterns such as `GlobalScope`,
`runBlocking`, `Thread.sleep` and `catch (Throwable)`. Treat findings in old code like lint:
- Fix a finding only when the fix can't change behavior.
- Otherwise, record it in `scripts/policy-baseline.txt` as
  `path: message  # TODO(migration)`, using the exact text the check prints. Then list it in
  `MIGRATION.md`.

Entries have no line numbers. An entry that stops matching fails the check, so the baseline can
only shrink.

Never delete a rule to get green.

Then:
1. Apply the application convention plugin to `app`. The convention reads `minSdk`/`targetSdk`
   from the version catalog, so set them there to the app's current values. Don't adopt the
   template's numbers.
2. Turn on lint `warningsAsErrors` and record existing findings with `./gradlew updateLintBaseline`.
   New findings now fail the build, and old ones are debt you can see.
3. Move SDK ids to `gradle.properties` if the owner chose to.
4. Run `verify.sh`, then commit.

## Phase 3 — Hilt

1. Apply the `<app>.hilt` plugin. Add `@HiltAndroidApp` to the `Application` and
   `@AndroidEntryPoint` to every Activity that gets a ViewModel.
2. Replace each manual singleton `object` with a `@Module @InstallIn(SingletonComponent::class)`
   (model: `core/network/.../di/NetworkModule.kt` and `core/data/.../di/DataModule.kt` in this repo)
   that builds **the same objects**: the same OkHttp clients and interceptors, the same
   SharedPreferences file names, the same cookie storage. A renamed preferences file signs every
   user out.
3. ViewModels become `@HiltViewModel` with an `@Inject constructor`. Replace `Application` or
   `Context` parameters with small injected interfaces. For example, the pilot migration added
   `ContentReader` to read a picked URI and `DeviceLocaleProvider` for the country.
4. App-wide signals such as "401 → sign out" become an injected singleton exposing a `SharedFlow`.

Do this **before** splitting modules. Manual singletons usually reach from the network layer
up into data and features, which shows up as dependency violations in Phase 5.

## Phase 4 — Split large ViewModels

Module lines follow ViewModel lines, so split first.

1. Group the old ViewModel's state and functions by the screen that uses them.
2. Create one ViewModel per group, shaped like `feature/notes/.../NoteEditorViewModel.kt`: one
   `StateFlow` of UI state, plain functions for events, no `Context`. A root composable forwards lifecycle calls (session bound, tab
   selected, resume) to each new ViewModel.
3. Share state through the repository or through events, never by holding a reference to
   another ViewModel.
4. Flows that outlive a screen, such as checkout with payment callbacks into the Activity, get an
   activity-scoped ViewModel.
5. Run the characterization tests and write down every intentional difference.

## Phase 5 — Modules

### Target layout

Use this template's modules. Leave out the ones the app doesn't need:

| Module | Template role |
| --- | --- |
| `app` | `Application`, `MainActivity`, navigation shell, app theme |
| `core:common` | Dispatchers, `suspendRunCatching`, `UiText`, `EventQueue`, validation |
| `core:model` | Plain Kotlin models |
| `core:network` | OkHttp/Retrofit, interceptors |
| `core:database` / `core:datastore` | Room, preferences |
| `core:data` | Repositories, API services, data sources, the Hilt data module |
| `core:designsystem` | Theme, components, `UiText.asString()`, **all resources** except launcher icons and app theme |
| `core:testing` | Fakes and sample data |
| `feature:<area>` | Screens and ViewModels for one area |
| `benchmark` | Baseline Profile and startup benchmark |

Migrated apps often need two more modules that the template doesn't ship:
- `core:navigation`: `NavKey`s and deep-link parsing shared by several features.
- `core:ui`: presentation code used by two or more features (shared state models, sheets, error
  mappers).

Add them only when the dependency analysis shows code with several feature users. Features never
depend on each other; the feature convention plugin fails the build if they do.

### Plan with the scripts

The scripts in [`scripts/migration/`](../scripts/migration) are driven by a JSON config:

1. Copy `migration.example.json` next to the project (outside `src`). Then set:
   - `repo` and `base_package`;
   - `module_levels` (lower levels may not use higher ones);
   - `anchors`: regexes on file paths, where the first match wins and `null` means "place by usage";
   - `overrides`, `feature_implicit_deps` and `library_imports`.
2. Dry run, which changes nothing:

   ```bash
   python3 scripts/migration/plan_modules.py --config ../my-app.migration.json
   ```

   It prints module sizes and every violation, meaning a lower module using a higher one or a
   feature using another feature. It writes `assignment.json` and `module_deps.json` next to the
   config. Fix violations by adding overrides, or by moving a type down (a model the data layer
   uses belongs in `core:data` or `core:model`). Repeat until there are none.

### Extract

```bash
python3 scripts/migration/extract_modules.py --config ../my-app.migration.json          # plan
python3 scripts/migration/extract_modules.py --config ../my-app.migration.json --apply  # git mv
python3 scripts/migration/prune_wildcards.py --config ../my-app.migration.json
```

This moves files with `git mv`, keeping history and the old package names. It also:
- moves resources to `core:designsystem` and points `R` imports at its `R`;
- gives modules that read `BuildConfig` their own;
- writes each module's `build.gradle.kts` from the convention plugins and detected dependencies;
- registers the modules in `settings.gradle.kts`.

Review every generated build file, and move `buildConfigField`s yourself. Then:

1. Add the modules to `app/build.gradle.kts`.
2. Compile one module at a time, lowest level first. When a compile reports
   "Cannot access … it is internal", run
   `python3 scripts/migration/fix_internal.py --config … build.log` and repeat. It makes exactly
   those declarations public and nothing else.
3. Keep manifest entries and `proguard-rules.pro` in `app`. Smoke-test a release build.
4. Lint findings move with the code. Give each new module its own `lint-baseline.xml`
   (`./gradlew :<module>:updateLintBaseline`) and regenerate `app`'s. Check that the total number
   of baselined issues didn't grow.
5. Run `verify.sh`, then commit.

### Script limits

The scripts have been run end to end on one project (the pilot migration). Know their limits:

- **Kotlin only.** Java files are neither planned nor moved. Convert them first, or move them by
  hand using the same assignment.
- **One source module.** They split one module (`source_module`, usually `app`). Apps that are
  already multi-module need one config per source module, or manual work.
- **`main` only.** The extractor moves `src/main`. Flavor and build-type source sets
  (`src/<flavor>/`) must be moved by hand to the same module. `rename_packages.py` does handle all
  source sets.
- **Text-based analysis.** References through reflection, strings (navigation routes, Gson class
  names) or XML (`android:name`, layouts) are invisible to the planner. Search for them before
  moving.
- **Generated build files are a starting point.** `buildConfigField`s, SDK repositories, packaging
  options and `exclude(...)` rules must be carried over by hand.

## Phase 6 — Rename packages to match modules

Set `package_rules` in the config: each rule has a module regex, a package pattern and a
replacement, and `{module_name}` is the part after the colon. Then:

```bash
python3 scripts/migration/rename_packages.py --config ../my-app.migration.json          # mapping + collisions
python3 scripts/migration/rename_packages.py --config ../my-app.migration.json --apply
```

The dry run stops on **collisions**, meaning two declarations with the same name would land in
the same package. Each one is real and needs a decision. The pilot migration had two: a dead
duplicate Activity (deleted) and two `toMessageRes()` helpers (one renamed).

`--apply` rewrites package lines, fully-qualified names and imports, expands split wildcard
imports, adds imports for same-package references that moved, and updates manifest class names
and ProGuard rules. It prints `CHECK` for any ProGuard rule whose package was split; fix those by
hand. Tests follow the package of what they test. Then:

1. Move tests into the module they test, and shared fakes into `core:testing`. Tests that
   exercise several feature ViewModels together stay in `app`.
2. Clean the affected modules. Stale KSP output causes "…_GeneratedInjector.java (No such file)"
   and lint crashes.
3. Run `verify.sh`, then commit.

## Phase 7 — Text out of code

1. **Composable text → resources** in `core/designsystem/src/main/res/values/strings_<module>.xml`.
   Prefix keys by module (`account_…`). A module uses only its own keys: duplicate a string
   rather than borrow another module's key. Counts become `<plurals>` (lint `PluralsCandidate`).
   Use `…` instead of `...`.
2. Never start or end a string value with `\"`, because Android shows the escaped quote
   literally. `check_policies.py` rejects it.
3. One sentence is one string. Use `%1$s` placeholders for links and names, never glued pieces.
4. **Non-composable text.** The template's pattern for new code is a typed value in UI state,
   mapped to a string in the composable (`NoteEditorError` → `stringResource`). When a migrated
   ViewModel builds many different messages, put `UiText` (`core:common`) in state instead:
   - Composables resolve it with `asString()` from `core:designsystem/text`.
   - Effects use `asString(LocalResources.current)`. Lint rejects `LocalContext.current.getString`.
   - Validators return typed errors; a mapper turns them into messages.
   - Data-layer errors become typed (`NoInternet`, `RateLimited`), never English.
5. Tests keep asserting the **visible English**: under Robolectric, resolve the `UiText`
   (`asString(RuntimeEnvironment.getApplication())`) instead of comparing resource ids.
6. Logic never compares display text (`if (status == "The host has ended…")`). Use a flag or an
   enum in state.

## Phase 8 — One-off events

For new code, use the template pattern: an event is state, and the UI calls back once it has been
shown or acted on (`onErrorConsume` in `NoteEditorScreen`). To migrate many `Channel` +
`receiveAsFlow()` events at once, replace each `Channel` with `EventQueue` from `core:common`:

```kotlin
private val _events = EventQueue<HomeEvent>()
val events: Flow<HomeEvent> = _events.events   // collectors stay unchanged
```

`send` never suspends. An event stays pending until its handler finishes, so an event interrupted
by a configuration change is delivered again instead of lost. Keep one collector per queue.

## Phase 9 — Platform and tooling

- **targetSdk:** raise it one level at a time. Read that release's behavior changes, and turn each
  one that affects the app into a device-checklist line. For Android 17 (API 37):
  - large screens ignore orientation and resizability locks;
  - background audio is restricted;
  - native code loading is stricter;
  - Certificate Transparency is on by default.
- **Baseline Profile:**
  - Copy this template's `benchmark/` module.
  - Apply `androidx.baselineprofile` and `profileinstaller` to `app`, and add
    `<profileable android:shell="true"/>`.
  - Sign benchmark-only build types with the debug key in `androidComponents.finalizeDsl`. Release
    signing stays untouched.
  - Generate the profile on a device with `./gradlew :app:generateReleaseBaselineProfile`, then
    commit it.
- **Formatting, last and as its own commit:**
  1. Copy the template's Spotless block.
  2. Point targets at a pruned `fileTree(rootDir) { include(…); exclude("**/build/**") }`.
  3. Run `spotlessApply`.
  4. ktlint can't fix some rules automatically: renames, file names, composable parameter order,
     primitive state in `rememberSaveable`. Disable those through
     `ktlint(...).editorConfigOverride(mapOf("ktlint_compose_param-order-check" to "disabled", …))`
     in the Spotless block. Entries in `.editorconfig` were **not** applied by Spotless 8.10 in
     the pilot migration.
  5. Disable rules in rounds: new ones appear once earlier failures stop masking them.
  6. Add `spotlessCheck` to `verify.sh`, and list the disabled rules as debt.

## Merging upstream while migrating

Other developers keep committing to the old layout. To bring their work in:

```bash
git merge -X find-renames=30% origin/<their-branch>
```

The lower threshold lets Git follow files that moved *and* changed package. Then:
- resolve conflicts in the new location;
- move new composable text to the module's strings file;
- place any new files using the module rules;
- run `verify.sh`.

## Working with AI agents

Agents work well on per-module mechanical tasks, such as one module's strings, one feature's
events or one area's characterization tests. Give each agent:
- the files it owns and the files it must not touch;
- the exact Gradle task that must pass;
- a rule against widening scope.

Review every diff yourself. Mistakes caught in the pilot migration:

- An agent injected a `Context` into a ViewModel to build a string. Use a resource id or `UiText`.
- An agent removed helpers as "dead code" that composables still used. Search for references
  before accepting any deletion.
- A text-fidelity check compared characters but missed escaped quotes, so users would have seen
  literal `\"`.
- A shell `cd` into a missing directory wrote files into another repository. Use absolute paths
  and `cd X || exit 1`.

## Pitfalls

| Symptom | Cause | Fix |
| --- | --- | --- |
| Gradle daemon killed or `OutOfMemoryError` | Native SDKs packaged across many modules | `org.gradle.jvmargs=-Xmx4g` |
| `…_GeneratedInjector.java (No such file)` in KSP or lint | Stale generated sources after moves | Clean the module or rerun; it is not a code error |
| Robolectric `IllegalAccessError` on JDK 21+ | Missing JVM flags | Already in `KotlinAndroid.kt`; keep them when copying |
| Gradle 9 fails a module's test task with no tests | Empty test sources after moving tests | Add a test, or set `failOnNoDiscoveredTests = false` |
| Literal `\"` or `%1$s` in the UI | Escaped quote at string edges; missing format argument | Policy check, plus a test for each format string |
| `LocalContextGetResourceValueCall` lint error | `LocalContext.current.getString` in composition | `LocalResources.current` |
| Users signed out after the update | Preferences or cookie file renamed in DI | Keep the exact file names |
| Crash only in the release build | ProGuard rules still name old packages | Check the `CHECK` lines from `rename_packages.py` |
| Policy check fails on legacy code | Template bans `GlobalScope`, `runBlocking`, … | Fix if behavior-neutral, else `scripts/policy-baseline.txt` |
| Spotless reports rules you disabled | `.editorconfig` `disabled` entries not applied | `editorConfigOverride` in the Spotless block |

## The record for testers

The migrated repo gets two docs:
- `docs/FEATURES.md`: every feature as a smoke-test checklist.
- `docs/MIGRATION.md`, containing:
  1. The decisions from Phase 0.
  2. A module table (module → contents).
  3. **Changes worth testing on a device**, each with exact steps (sign in, kill the app, reopen;
     force a 401; payment success and failure; every tab; background and return).
  4. **Visible differences** (for example singular plurals, a field no longer pre-filled,
     "2 hours" instead of "2 hour").
  5. The platform checklist (targetSdk changes, large screens, background audio, SDKs).
  6. Follow-ups and debt: lint baselines and disabled format rules.

Describe validation exactly: static, compiled, unit-tested, or device-tested on a named device
and flavor. A green `verify.sh` proves the build, not the behavior. The owner's device test is the
final gate.

## Checklist

- [ ] 0 Inventory written; owner decisions recorded
- [ ] 1 Branch + snapshot; baseline build; characterization tests green
- [ ] 2 `build-logic`, version catalog, `verify.sh`, lint baselines
- [ ] 3 Hilt builds the same objects; no `Context` in ViewModels
- [ ] 4 Large ViewModels split; characterization tests green
- [ ] 5 `plan_modules.py` reports zero violations; modules extracted; release build smoke-tested
- [ ] 6 `rename_packages.py` applied with zero collisions; `CHECK` lines resolved
- [ ] 7 Strings in resources; `UiText` or typed state; no logic on display text
- [ ] 8 One-off events are state or `EventQueue`
- [ ] 9 targetSdk with device checklist; Baseline Profile; formatting commit
- [ ] `MIGRATION.md` and `FEATURES.md` handed to the owner for device testing

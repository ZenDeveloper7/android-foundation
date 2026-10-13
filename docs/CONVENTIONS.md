# Coding conventions

The checklist reviewers and agents use. Each rule links to the Google guidance it comes from, or
names the defect it prevents (several come from a review of an earlier codebase).

## Kotlin

- Follow the [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide):
  4-space indent, 100 columns, no wildcard imports. `./gradlew spotlessApply` fixes most of it.
- No `!!` in production code. Model absence with nullable types or sealed results.
- Never wrap suspend code in plain `runCatching`: it swallows `CancellationException`. Use
  `suspendRunCatching` from `:core:common`, or catch specific exceptions.
- Never catch `Throwable`, and never ignore an exception without a comment saying why.
- Inject dispatchers (`@Dispatcher(IO)`) and time (`Clock`); don't reference them statically.
- No `GlobalScope`, `runBlocking` or `Future.get()` in app code.
- Keep files focused. If a file passes about 400 lines, split by responsibility rather than
  compressing code to stay under a limit.

## Compose

Based on [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
and [Compose performance](https://developer.android.com/develop/ui/compose/performance); the Compose
ktlint rules enforce many of them.

- Screens come in pairs: `XRoute` (connects the ViewModel) and stateless `XScreen` (state in,
  events out). Pass state and lambdas down, never a ViewModel.
- Every public composable takes `modifier: Modifier = Modifier` as its first optional parameter.
- Collect flows with `collectAsStateWithLifecycle()`.
- Lazy lists use stable `key`s.
- Every shared component and screen has `@PreviewFoundation` (light, dark, 200% font) using
  `FoundationTheme(dynamicColor = false)`.
- Colors come from `MaterialTheme.colorScheme`, spacing from `FoundationTheme.spacing`. No raw hex
  colors in features.
- Screen state objects stay small. If a `UiState` passes about 15 fields, split it by section.

## Accessibility

Based on [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility).

- Icon-only buttons need a `contentDescription`. Decorative icons next to a text label use `null`.
- Make a whole row the click target (`selectable`, `toggleable`) with the right `Role`, not only
  the small control inside it.
- Mark section titles with `semantics { heading() }`.
- Check layouts at 200% font scale (the multipreview includes it) and in right-to-left languages.

## Resources and strings

- All user-facing text lives in `strings.xml`. Prefix resource names with the module
  (`feature_notes_…`) so modules never collide.
- Use `<plurals>` for any count. Lint fails on missing plural forms (`MissingQuantity`).
- When adding a language, translate every string. Lint fails on missing translations.

## Security and privacy

Based on the [Android security checklist](https://developer.android.com/privacy-and-security/security-tips).

- Only the launcher activity is exported. A new exported component must validate every extra and
  must not perform privileged actions (calls, sends, file access) for an unknown caller.
- Use immutable `PendingIntent`s unless mutability is required and documented.
- HTTPS only. Add a scoped `<domain-config>` if a specific host truly needs cleartext.
- Secrets never go in source, `BuildConfig` or logs. Logging of network traffic is debug-only and
  redacts `Authorization` and `Cookie`.
- Request permissions in the manifest of the module that needs them, and degrade gracefully when
  denied.
- Backup is an allowlist. New files containing tokens or device-specific state stay off it.

## Data

- Model each API response from its actual contract. Don't share one model between endpoints that
  only look similar.
- Room: per-row suspend writes, `Flow` reads, a migration for each schema change.
- DataStore key names and Room column names are on-disk formats: never rename without a migration.

## Enforced by scripts

Gradle, lint and ktlint don't check these. `scripts/check_policies.py` (run by `scripts/verify.sh`)
enforces the banned-call, ViewModel-import, feature-import, exported-component, cleartext, backup
and release-signing rules above and in the Security section.

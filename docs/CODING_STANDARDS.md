# Android coding standards

These standards define review expectations and the initial automated gates. Google recommendations
are adapted to the product; optional libraries are not treated as compliance certificates.

## Kotlin and maintainability

- Four-space indentation, normally 100 columns, explicit imports and consistent file/class names.
- Spotless + ktfmt Kotlin style is the executable formatting policy; `.editorconfig` matches it.
- Prefer small cohesive functions/classes and readable constructor wiring. Do not compress statements
  to satisfy an arbitrary file-line cap. Extract around responsibility, not line count alone.
- Prefer immutable models, private mutable state and explicit public contracts. Do not expose a
  MutableStateFlow, Room entity, database handle or transport response as screen state.
- Use nullability and sealed types to express actual states. Avoid `!!`, unchecked casts, empty catch
  blocks, speculative generic abstractions and comments that simply restate the code.
- Android resources contain UI copy and formatting. Add required locale plural categories, RTL-safe
  layout and consistent format arguments when adding translations. Do not infer translation quality
  from key parity alone.

## Architecture and UI state

- Separate UI, state ownership, data access and platform integrations. Constructor injection is the
  default; context belongs at the Android/data boundary, not in ViewModels.
- Screen ViewModels expose `StateFlow` and action methods. Reusable components use plain state
  holders and callbacks. State collection respects the owner's lifecycle.
- Durable UI outcomes are state, not an unreliable fire-and-forget event channel. Keep navigation
  callbacks at the UI boundary. Save minimal restorable input and destination IDs.
- Introduce use cases/modules/interfaces where there is a demonstrated responsibility or reuse.
  Do not require a domain layer or API/implementation module pair for every tiny feature.
- APIs state lifetime and main-safety expectations. Expose observable reads and suspend actions.

## Coroutines and persistence

- Use structured concurrency and explicit owners. No GlobalScope, runBlocking, Thread.sleep or
  blocking Future.get in production Android paths. Do not swallow cancellation.
- Make repositories main-safe. Room suspend queries already manage execution; do not wrap them in
  blocking executors. Inject dispatchers for custom blocking work and deterministic tests.
- Keep search/debounce publication versioned where old computations can finish after cancellation.
  Add a late-result race test before relying on cancellation alone.
- Use per-record mutations; transactionally protect multi-record invariants. Never replace a whole
  database table merely to edit one record. Use one authoritative persistence store.
- Export Room schemas and add migration tests for every version bump. Destructive migration is an
  explicit product/data-retention decision, never the default workaround for a migration failure.
- Observe persisted success before clearing user input. Test concurrent writes, retries, failures,
  state restoration and cancellation. Give truly durable operations an appropriate durable owner.

## Compose, adaptation and accessibility

- Prefer Material 3 and shared theme tokens. Hoist state; keep composables free of blocking I/O.
- Scope effects to the appropriate lifecycle; avoid using onDispose to synchronously save data.
- Respect Scaffold/system/IME insets once. Validate gesture and three-button navigation, keyboard,
  cutouts and system-bar contrast on a device.
- Adapt to available window space and input, not only a phone/tablet label. Do not lock orientation
  unless the product has a documented essential reason. Use stable, appropriately sized layouts.
- Use stable lazy item keys. Preserve focus and saveable state when layouts resize.
- Give actionable icons/controls meaningful labels, role/state semantics and suitable touch targets.
  Decorative images have no spoken label. Test destructive confirmation and keyboard navigation.
- Add phone/tablet/large-text previews. Validate compact/expanded widths, font scale, RTL, keyboard
  and TalkBack. Preview annotations and Robolectric layout tests are not pixel or physical-device proof.

## Security, privacy and backup

- Export only documented entry points. Treat all external intents, URIs and extras as untrusted;
  validate allowed actions and destinations and authorize use of sensitive app permissions.
- Prefer immutable PendingIntents, narrowly scoped URI grants and private providers. Add negative
  tests for any externally reachable action handler before exposing it.
- Use HTTPS by default. Never install a trust-all manager or globally enable cleartext to solve a
  local backend problem. Scope and disclose any justified exception.
- Do not store server secrets in mobile code/resources, Git, build logs or BuildConfig. A mobile app
  cannot conceal a bundled secret. Redact tokens and personal data from diagnostics.
- Storage/encryption follows the product's threat model and current Android Keystore/provider APIs.
  Avoid deprecated encryption wrappers for new code and do not silently fall back for credentials.
- Backup is an explicit data policy. Automatic rules operate on files, not arbitrary preference keys.
  Separate transferable settings from secrets/transient data and test cloud/device restoration.
- Portable export of personal context, keys or other sensitive data requires deliberate selection and
  disclosure or appropriate encryption. A generic settings export must not silently include it.
- Request only needed permissions at the feature boundary; provide a usable denied-permission path.

## Testing and delivery

- CI must pass formatting, module policies, unit/behavior tests, lint and debug/minified release builds.
  No blanket lint suppressions or generated baseline to conceal a new issue. Only dependency-version
  notification rules are ignored, with a rationale in `lint.xml`: reviewed Dependabot updates own
  version drift; compatibility and source checks remain enabled.
- Prefer fakes, deterministic clocks/locales and test schedulers. No sleeps, real credentials or live
  paid/provider endpoints in normal tests. Test contracts and failure behavior, not duplicate code.
- Test ViewModel state transitions, persistence/migrations, relevant navigation/back behavior,
  interaction semantics and restoration. Add device/end-to-end/performance tests for real integrations.
- Coverage is evidence of exercised code, not proof of correctness. Add useful scenarios before
  selecting a numeric gate; exclude generated code rather than pretending it is business logic.
- Keep dependencies pinned and updates reviewed. Pin CI actions to commits, use read-only workflow
  permissions and keep signing material in the consuming app's protected release process.
- Never sign a production release with a debug key or invent fallback signing credentials. The
  foundation emits an unsigned minified release to verify R8 without possessing a keystore.

## Review checklist

1. Is the responsibility in the right layer, with a testable contract and an appropriate lifetime?
2. Are failure, cancellation, restore, concurrent work and permission denial handled?
3. Are UI copy, adaptation, accessibility, insets and input behavior covered at the right test level?
4. Does the change alter export, transport, logging, backup, privacy or signing boundaries?
5. Do required checks pass, and does the report clearly separate static/local evidence from device proof?

Sources: [Kotlin style](https://developer.android.com/kotlin/style-guide),
[architecture](https://developer.android.com/topic/architecture/recommendations),
[coroutines](https://developer.android.com/kotlin/coroutines/coroutines-best-practices),
[Room async queries](https://developer.android.com/training/data-storage/room/async-queries),
[accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults),
[adaptive UI](https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps),
[intent risks](https://developer.android.com/privacy-and-security/risks/intent-redirection),
[backup](https://developer.android.com/identity/data/autobackup),
[signing](https://developer.android.com/studio/publish/app-signing).

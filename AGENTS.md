# Working in Android Foundation

Read `docs/CODING_STANDARDS.md`, `docs/ARCHITECTURE.md` and `docs/TESTING.md` before implementation.
Trace the actual flow before modifying it. Keep changes narrow and preserve unrelated work.

- Follow UI -> repository boundaries and the initial dependency graph. ViewModels have no Context.
- Keep suspend persistence main-safe; do not add blocking waits or full-table edit operations.
- All visible copy belongs in resources. Preserve accessibility semantics and saved-state behavior.
- Treat exported inputs, transport, backup and signing as explicit tested product boundaries.
- Run `./gradlew spotlessApply`, then `./gradlew verify` before reporting completion.
- Do not suppress lint, replace meaningful assertions or add blanket R8 keep rules to hide failures.
- If a new integration requires device QA, state what was actually run; compilation is not UI proof.
- Keep credentials, keystores, local.properties, build outputs and private logs out of commits.
- Do not commit/push/publish or mutate remote settings without user authorization. Initial repository
  creation/publication may be authorized by the current task; subsequent work follows its own scope.
- Follow applicable user/global instructions for tools and classifications; no external AI is needed
  by the application runtime or its tests.

# Starting a new app

1. Copy the template into a new repository (GitHub "Use this template", or copy the folder and
   `git init`). Commit it unchanged first so the rename is a reviewable diff.
2. Rename the package, application id and name:

   ```bash
   python3 scripts/rename_app.py --package com.acme.notes --name "Acme Notes" --dry-run
   python3 scripts/rename_app.py --package com.acme.notes --name "Acme Notes"
   ```

3. Replace the brand: the seed colors in `core/designsystem/.../theme/Color.kt` (generate both
   schemes with the Material Theme Builder), `ic_launcher_foreground.xml` and
   `ic_launcher_background` in `app/src/main/res`.
4. Set `API_BASE_URL` in `core/network/build.gradle.kts`, or delete `:core:network` if the app has
   no backend (it is the only module that adds the `INTERNET` permission).
5. Replace the sample features: keep one until your first real feature works as a reference, then
   delete `:feature:notes`, its `NoteEntity`/`NoteDao` and its repository. Remove
   `foundation.db` entries from the backup rules if you rename the database.
6. Run `scripts/verify.sh` and push. CI runs the same script.

## Adding a feature

1. Create `feature/<name>/build.gradle.kts` with the `foundation.android.feature` plugin and a
   namespace, and add `include(":feature:<name>")` to `settings.gradle.kts`.
2. Add `NavKey`s and an `EntryProviderScope.<name>Entries(...)` function.
3. Add `XScreen`/`XRoute`/`XViewModel`, strings prefixed `feature_<name>_`, previews and tests.
4. Register the entries in `app/.../ui/FoundationApp.kt`; add a `TopLevelDestination` if it
   belongs in the navigation bar.

## Adding data

- New table: entity + DAO in `:core:database`, bump the database version with a migration, and
  commit the exported schema.
- New repository: interface and `internal` implementation in `:core:data`, a `@Binds` in
  `DataModule`, and a fake in `:core:testing`.
- New endpoint: a Retrofit interface in `:core:data` (created from the injected `Retrofit`), with
  `@Serializable` models matching that endpoint's actual response.

# Start a new app

1. Create a repository using this GitHub template. Keep the MIT license and upstream wrapper notices
   when retaining code. Rename the repository/root project and replace the example package prefix
   `dev.zen.foundation`, app ID, app label and module names with your own identity.
2. Read the standards and architecture before adding features. Keep one complete vertical feature
   until its behavior is covered; then remove or replace the task sample together with its tests.
3. Define supported devices, minimum SDK, languages, offline/error behavior, accessibility and
   data-retention/backup policy. Do not interpret the starter's minimum SDK or backup choices as a
   mandatory policy for all products.
4. Add backend/auth/work/platform integrations only where needed, with repositories, appropriate
   lifetimes, denied/error paths and fakes. Use DataStore for non-sensitive preferences, not for
   server secrets. Do not pre-install billing/analytics/provider SDKs "just in case".
5. Rename/update the architecture-check module graph with intentional module changes. Update tests
   and architecture documentation; do not disable the gate to bypass an unexplained violation.
6. Enable branch protection on main and require the `verify` workflow status before merge. Review
   dependency update PRs and rerun checks; templates do not inherit repository protection settings.
7. Configure production signing in protected CI, Play App Signing/upload identity as applicable,
   release versioning and distribution. The foundation release remains unsigned and is not an
   installable production artifact until the consuming app adds its signing process.
8. Add screenshot references, device migration/restore tests and baseline/macrobenchmark journeys
   for the actual product. Record what was measured and where. Run the device checklist before release.

For SDK/dependency updates, check the official release notes and compatibility requirements, update
the version catalog/wrapper, and rerun the full gate. Prefer stable versions; experimental APIs need
a documented reason and a migration plan. Never assume "latest" means compatible.

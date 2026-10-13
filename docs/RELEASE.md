# Releasing

## Signing

Release builds are signed only when these environment variables are set (CI secrets or your
shell). Otherwise the release APK/AAB is unsigned. It never falls back to the debug key.

| Variable | Value |
| --- | --- |
| `FOUNDATION_KEYSTORE_FILE` | Path to the upload keystore |
| `FOUNDATION_KEYSTORE_PASSWORD` | Keystore password |
| `FOUNDATION_KEY_ALIAS` | Key alias |
| `FOUNDATION_KEY_PASSWORD` | Key password |

Use [Play App Signing](https://developer.android.com/studio/publish/app-signing#app-signing-google-play):
the key above is an upload key, and Google holds the app signing key. Keystores are git-ignored;
never commit them.

The Baseline Profile plugin's local `benchmarkRelease` and `nonMinifiedRelease` builds use the
debug key so they can be installed for measurement. They are never distributed.

## Checklist

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Regenerate the Baseline Profile on a device: `./gradlew :app:generateReleaseBaselineProfile`,
   then commit the updated profile.
3. `scripts/verify.sh` passes.
4. Build with signing: `./gradlew :app:bundleRelease`.
5. Smoke-test the minified release build on a device (R8 problems only show up there).
6. Tag the release commit.

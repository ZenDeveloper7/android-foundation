#!/usr/bin/env bash
# Definition of done for any change: run this and get "VERIFY PASSED". CI runs the same script.
#
#   scripts/verify.sh            repository policy checks, formatting, lint, unit + Robolectric UI
#                                tests, debug and release builds, benchmark compilation
#   scripts/verify.sh --quick    policy checks, formatting and unit tests only (for fast iteration)
#
# The policy checks (scripts/check_policies.py) run first, before Gradle, in both modes.
# Extra arguments are passed to Gradle (for example --offline).
set -euo pipefail
cd "$(dirname "$0")/.."

quick=false
gradle_args=()
for arg in "$@"; do
    case "$arg" in
        --quick) quick=true ;;
        *) gradle_args+=("$arg") ;;
    esac
done

python3 scripts/check_policies.py

tasks=(spotlessCheck testDebugUnitTest :core:model:test)
if [[ "$quick" == false ]]; then
    tasks+=(lintDebug :core:model:lint :app:assembleDebug :app:assembleRelease
        :benchmark:compileBenchmarkReleaseKotlin)
fi

./gradlew --console=plain -PwarningsAsErrors=true "${tasks[@]}" "${gradle_args[@]+"${gradle_args[@]}"}"
echo "VERIFY PASSED"

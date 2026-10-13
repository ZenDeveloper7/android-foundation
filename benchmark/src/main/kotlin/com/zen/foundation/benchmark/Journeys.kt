package com.zen.foundation.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope

/**
 * Release application id (benchmark builds have no `.debug` suffix).
 * Updated by scripts/rename_app.py.
 */
const val TARGET_PACKAGE = "com.zen.foundation"

/** Cold start to the first screen. Add your app's critical journeys next to this one. */
fun MacrobenchmarkScope.startToNotes() {
    pressHome()
    startActivityAndWait()
}

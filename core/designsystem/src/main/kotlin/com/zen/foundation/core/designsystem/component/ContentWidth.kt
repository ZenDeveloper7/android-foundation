package com.zen.foundation.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Readable line length on tablets, foldables and desktop windows (Material's large breakpoint). */
private val MaxContentWidth = 840.dp

/**
 * Centers content and caps its width on large screens; on phones it fills the width.
 * Apply to each screen's main content inside its Scaffold.
 */
fun Modifier.foundationContentWidth(): Modifier =
    fillMaxWidth().wrapContentWidth().widthIn(max = MaxContentWidth)

package com.zen.foundation.core.designsystem.text

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.zen.foundation.core.common.UiText

/** Resolves [UiText] in composition (configuration-aware). */
@Composable
fun UiText.asString(): String = asString(LocalResources.current)

/** Resolves [UiText] outside composition, for example in a notification. */
fun UiText.asString(context: Context): String = asString(context.resources)

/** Resolves [UiText] with explicit [Resources]; in effects, pass `LocalResources.current`. */
fun UiText.asString(resources: Resources): String = when (this) {
    is UiText.Resource -> resources.getString(id, *args.resolve(resources))
    is UiText.Plural -> resources.getQuantityString(id, count, *args.resolve(resources))
    is UiText.Raw -> value
}

// Arguments may themselves be UiText (for example a localized label inside a sentence).
private fun List<Any>.resolve(resources: Resources): Array<Any> =
    map { if (it is UiText) it.asString(resources) else it }.toTypedArray()

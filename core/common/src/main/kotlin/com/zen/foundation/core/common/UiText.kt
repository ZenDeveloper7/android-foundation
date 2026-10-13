package com.zen.foundation.core.common

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * User-visible text decided outside the UI, for example a message picked in a mapper or by a
 * migrated ViewModel. It holds a resource reference, never English; the UI resolves it with
 * `asString()` from `:core:designsystem`. Use [Raw] only for text from the server or the user.
 *
 * New screens usually don't need this: put a typed error in the UI state and map it to a string in
 * the composable (see `NoteEditorError`). Prefer [UiText] when one state field can hold many
 * different messages.
 */
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(
        @param:PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = listOf(count)
    ) : UiText

    data class Raw(val value: String) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Resource(id, args.toList())

fun uiPlural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText =
    UiText.Plural(id, count, if (args.isEmpty()) listOf(count) else args.toList())

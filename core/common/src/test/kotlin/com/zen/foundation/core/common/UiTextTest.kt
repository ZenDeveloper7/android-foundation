package com.zen.foundation.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UiTextTest {
    @Test
    fun equality_isByResourceAndArguments() {
        assertThat(uiText(1, "a")).isEqualTo(UiText.Resource(1, listOf("a")))
        assertThat(uiText(1, "a")).isNotEqualTo(uiText(1, "b"))
    }

    @Test
    fun plural_defaultsToCountAsArgument() {
        assertThat(uiPlural(2, 3)).isEqualTo(UiText.Plural(2, 3, listOf(3)))
    }
}

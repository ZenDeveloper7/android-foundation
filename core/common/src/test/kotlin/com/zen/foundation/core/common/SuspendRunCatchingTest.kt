package com.zen.foundation.core.common

import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SuspendRunCatchingTest {
    @Test
    fun success_isWrapped() = runTest {
        assertThat(suspendRunCatching { 42 }.getOrNull()).isEqualTo(42)
    }

    @Test
    fun exception_isCaptured() = runTest {
        val result = suspendRunCatching { error("boom") }
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun cancellation_isRethrown() = runTest {
        val thrown =
            try {
                suspendRunCatching { throw CancellationException("cancelled") }
                null
            } catch (cancellation: CancellationException) {
                cancellation
            }
        assertThat(thrown).isNotNull()
    }
}

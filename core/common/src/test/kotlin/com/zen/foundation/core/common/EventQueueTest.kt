package com.zen.foundation.core.common

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventQueueTest {
    @Test
    fun keepsEventsSentWithoutACollectorAndDeliversThemInOrder() = runTest {
        val queue = EventQueue<String>()
        queue.send("first")
        queue.send("second")

        assertThat(collectPending(queue)).containsExactly("first", "second").inOrder()
        assertThat(queue.pending.value).isEmpty()
    }

    @Test
    fun redeliversAnEventWhoseHandlingWasCancelled() = runTest {
        val queue = EventQueue<String>()
        queue.send("snackbar")
        val neverShown = CompletableDeferred<Unit>()

        val firstCollector = launch { queue.events.collect { neverShown.await() } }
        runCurrent()
        firstCollector.cancel()
        runCurrent()

        assertThat(queue.pending.value.map { it.event }).containsExactly("snackbar")
        assertThat(collectPending(queue)).containsExactly("snackbar")
        assertThat(queue.pending.value).isEmpty()
    }

    @Test
    fun deliversEventsSentWhileCollecting() = runTest {
        val queue = EventQueue<Int>()
        val received = mutableListOf<Int>()
        val collector = launch { queue.events.collect { received += it } }
        runCurrent()

        queue.send(1)
        runCurrent()
        queue.send(2)
        queue.send(3)
        runCurrent()

        assertThat(received).containsExactly(1, 2, 3).inOrder()
        collector.cancel()
    }

    private fun <E : Any> TestScope.collectPending(queue: EventQueue<E>): List<E> {
        val received = mutableListOf<E>()
        val collector = launch { queue.events.collect { received += it } }
        runCurrent()
        collector.cancel()
        return received
    }
}

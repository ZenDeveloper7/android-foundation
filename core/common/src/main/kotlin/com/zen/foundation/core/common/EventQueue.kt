package com.zen.foundation.core.common

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * One-off ViewModel events (snackbars, navigation) held as state until the UI has handled them.
 *
 * New screens should model these as UI state with a "consumed" callback (see `NoteEditorScreen`).
 * This queue is the drop-in replacement when migrating code that used `Channel` events: [send]
 * never suspends the ViewModel, and an event stays in [pending] until a collector of [events]
 * finishes handling it, so an event interrupted by a configuration change is delivered again
 * instead of lost. Use a single collector per queue.
 */
class EventQueue<E : Any> {
    private val nextId = AtomicLong()
    private val _pending = MutableStateFlow<List<Pending<E>>>(emptyList())

    /** Events not yet handled, oldest first. */
    val pending: StateFlow<List<Pending<E>>> = _pending.asStateFlow()

    /** Emits each pending event in order and removes it once the collector has handled it. */
    val events: Flow<E> =
        flow {
            _pending.collect { queued ->
                for (item in queued) {
                    if (_pending.value.none { it.id == item.id }) continue
                    emit(item.event)
                    consumed(item.id)
                }
            }
        }

    fun send(event: E) {
        val item = Pending(nextId.incrementAndGet(), event)
        _pending.update { it + item }
    }

    fun consumed(id: Long) {
        _pending.update { queued -> queued.filterNot { it.id == id } }
    }

    data class Pending<E>(val id: Long, val event: E)
}

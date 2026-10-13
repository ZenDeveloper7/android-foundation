package com.zen.foundation.core.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] for suspend code, but rethrows [CancellationException] so coroutine
 * cancellation keeps working. Never use plain `runCatching` around suspend calls.
 */
suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (exception: Exception) {
    Result.failure(exception)
}

package com.zen.foundation.core.common

import javax.inject.Qualifier

/**
 * Inject dispatchers instead of referencing `Dispatchers.IO` directly, so tests can replace them
 * with a test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: FoundationDispatcher)

enum class FoundationDispatcher {
    Default,
    IO
}

/** A scope that outlives any screen, for work that must finish even if the user leaves. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

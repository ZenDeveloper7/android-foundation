package com.zen.foundation.core.model

/** Input bounds shared by the UI (to stop typing) and the repository (to reject bad writes). */
object NoteLimits {
    const val MAX_TITLE_LENGTH = 200
    const val MAX_BODY_LENGTH = 20_000
}

package com.zen.foundation.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Preview every shared component and screen in light, dark and at the largest common font scale.
 * Previews must use `FoundationTheme(dynamicColor = false)`.
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(name = "Font 200%", showBackground = true, fontScale = 2f)
annotation class PreviewFoundation

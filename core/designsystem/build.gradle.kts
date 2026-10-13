plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.android.compose)
}

android {
    namespace = "com.zen.foundation.core.designsystem"
}

dependencies {
    api(projects.core.common)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}

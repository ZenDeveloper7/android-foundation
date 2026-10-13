plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.hilt)
}

android {
    namespace = "com.zen.foundation.core.common"
}

dependencies {
    api(libs.androidx.annotation)
    api(libs.kotlinx.coroutines.android)
}

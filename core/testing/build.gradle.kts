plugins {
    alias(libs.plugins.foundation.android.library)
}

android {
    namespace = "com.zen.foundation.core.testing"
}

dependencies {
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.truth)
    api(libs.turbine)
    api(projects.core.data)
    api(projects.core.model)
}

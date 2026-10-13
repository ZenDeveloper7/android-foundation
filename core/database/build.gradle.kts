plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.android.room)
    alias(libs.plugins.foundation.hilt)
}

android {
    namespace = "com.zen.foundation.core.database"
}

dependencies {
    implementation(projects.core.model)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}

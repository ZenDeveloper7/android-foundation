plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.hilt)
}

android {
    namespace = "com.zen.foundation.core.datastore"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.common)
    implementation(projects.core.model)

    testImplementation(libs.turbine)
}

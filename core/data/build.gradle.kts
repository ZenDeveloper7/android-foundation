plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.hilt)
}

android {
    namespace = "com.zen.foundation.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)

    testImplementation(libs.turbine)
}

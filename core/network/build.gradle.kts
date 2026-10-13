plugins {
    alias(libs.plugins.foundation.android.library)
    alias(libs.plugins.foundation.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.zen.foundation.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // Replace per app. HTTPS only: cleartext is blocked by the app's network security config.
        buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.retrofit)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.kotlinx.serialization)

    testImplementation(libs.okhttp.mockwebserver)
}

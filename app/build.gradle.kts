plugins {
    alias(libs.plugins.foundation.android.application)
    alias(libs.plugins.foundation.android.compose)
    alias(libs.plugins.foundation.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.zen.foundation"

    defaultConfig {
        applicationId = "com.zen.foundation"
        versionCode = 1
        versionName = "0.1.0"
    }

    androidResources {
        // Builds the per-app language list from res/values-* (Android 13+ language picker).
        generateLocaleConfig = true
    }

    // Release signing comes only from the environment (CI secrets or your shell). Without it the
    // release APK is unsigned: it never falls back to the debug key. See docs/RELEASE.md.
    val keystorePath = providers.environmentVariable("FOUNDATION_KEYSTORE_FILE").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("FOUNDATION_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("FOUNDATION_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("FOUNDATION_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

androidComponents {
    finalizeDsl { extension ->
        // Local-only performance builds created by the Baseline Profile plugin need some signature
        // to install. They are never distributed, so the debug key is acceptable here only.
        extension.buildTypes
            .matching { it.name == "benchmarkRelease" || it.name == "nonMinifiedRelease" }
            .configureEach { signingConfig = extension.signingConfigs.getByName("debug") }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.notes)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)

    baselineProfile(projects.benchmark)

    testImplementation(projects.core.testing)
}

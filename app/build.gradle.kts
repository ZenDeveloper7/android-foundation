plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.zen.foundation"

    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        warningsAsErrors = true
        lintConfig = rootProject.file("lint.xml")
    }
    testOptions { unitTests.isIncludeAndroidResources = true }

    defaultConfig {
        applicationId = "dev.zen.foundation"
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            // Deliberately unsigned: production signing belongs to the consuming app's CI.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(project(":feature:tasks"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.navigation)
    implementation(libs.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.compose.test)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.serialization.json)
}

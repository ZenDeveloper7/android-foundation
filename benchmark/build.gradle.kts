// Generates the Baseline Profile and measures startup. Run on a physical device or emulator:
//   ./gradlew :app:generateReleaseBaselineProfile
//   ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.zen.foundation.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Baseline Profile generation needs API 28+.
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}

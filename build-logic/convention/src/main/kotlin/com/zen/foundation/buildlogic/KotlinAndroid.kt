package com.zen.foundation.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** Bytecode level for every module. Raise it here only; CI's JDK may be newer. */
internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Shared Android configuration for application and library modules. */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.compileSdk = libs.intVersion("compileSdk")
    extension.defaultConfig.minSdk = libs.intVersion("minSdk")
    extension.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    extension.compileOptions.sourceCompatibility = JAVA_VERSION
    extension.compileOptions.targetCompatibility = JAVA_VERSION

    extension.lint.apply {
        // Lint is a release gate: new warnings fail the build instead of accumulating.
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
        lintConfig = rootProject.file("lint.xml")
        htmlReport = true
        sarifReport = true
    }

    extension.testOptions.unitTests.isIncludeAndroidResources = true
    extension.testOptions.unitTests.all { test ->
        // Robolectric reaches JDK internals for file descriptors; newer JDKs block that by default.
        test.jvmArgs(
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED"
        )
    }

    configureKotlin()
}

/**
 * Kotlin compiler settings for Android and JVM modules.
 * Warnings become errors when `-PwarningsAsErrors=true` is passed (CI and scripts/verify.sh do).
 */
internal fun Project.configureKotlin() {
    val warningsAsErrors =
        providers.gradleProperty("warningsAsErrors").map(String::toBoolean).orElse(false)
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.toString()))
            allWarningsAsErrors.set(warningsAsErrors)
        }
    }
}

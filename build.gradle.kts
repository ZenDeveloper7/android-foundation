plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktfmt("0.64").kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktfmt("0.64").kotlinlangStyle()
    }
    format("text") {
        target("**/*.md", "**/*.yml", "**/*.toml", ".editorconfig", ".gitignore")
        targetExclude("**/build/**", "**/.gradle/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register<Exec>("architectureCheck") {
    group = "verification"
    workingDir(rootDir)
    val python = if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3"
    commandLine(python, "scripts/check_architecture.py")
}

tasks.register("verify") {
    group = "verification"
    description = "Formatting, architecture policies, unit/UI tests, lint and debug/release builds."
    dependsOn(
        "spotlessCheck",
        "architectureCheck",
        ":app:lintDebug",
        ":core:data:lintDebug",
        ":core:designsystem:lintDebug",
        ":feature:tasks:lintDebug",
        ":app:testDebugUnitTest",
        ":core:data:testDebugUnitTest",
        ":feature:tasks:testDebugUnitTest",
        ":app:assembleDebug",
        ":app:assembleRelease",
    )
}

// Root build: declares plugins for the whole build (apply false) and repository-wide checks.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = "io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"
    kotlin {
        // A pruned tree, so Spotless never walks module build output.
        target(
            fileTree(rootDir) {
                include("**/src/**/*.kt")
                exclude("**/build/**", ".gradle/**", ".kotlin/**")
            }
        )
        ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
    }
    kotlinGradle {
        target(
            fileTree(rootDir) {
                include("**/*.gradle.kts")
                exclude("**/build/**", ".gradle/**", ".kotlin/**")
            }
        )
        ktlint(ktlintVersion)
    }
    format("misc") {
        target("**/*.md", "**/*.toml", "**/*.yml", "**/*.xml", ".editorconfig", ".gitignore")
        targetExclude("**/build/**", "**/.gradle/**", "**/.idea/**", "**/schemas/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

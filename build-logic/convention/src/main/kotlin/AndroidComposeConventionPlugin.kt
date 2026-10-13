import com.android.build.api.dsl.CommonExtension
import com.zen.foundation.buildlogic.library
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** Enables Compose on a module that already applies the application or library convention. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true

            dependencies {
                val bom = libs.library("androidx-compose-bom")
                "implementation"(platform(bom))
                "testImplementation"(platform(bom))
                "androidTestImplementation"(platform(bom))

                "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
                "debugImplementation"(libs.library("androidx-compose-ui-tooling"))

                // Compose UI tests run on the JVM through Robolectric; see docs/TESTING.md.
                "testImplementation"(libs.library("androidx-compose-ui-test-junit4"))
                "testImplementation"(libs.library("robolectric"))
                "debugImplementation"(libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}

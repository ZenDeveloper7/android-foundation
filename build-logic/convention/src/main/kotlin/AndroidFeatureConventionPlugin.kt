import com.zen.foundation.buildlogic.library
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * A feature module: one user-facing area with its screens, ViewModels and navigation keys.
 * Features depend on core modules only; they never depend on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "foundation.android.library")
            apply(plugin = "foundation.android.compose")
            apply(plugin = "foundation.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":core:data"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:model"))

                "implementation"(libs.library("androidx-activity-compose"))
                "implementation"(libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-navigation3-runtime"))
                "implementation"(libs.library("kotlinx-serialization-json"))

                "testImplementation"(project(":core:testing"))
            }

            val featurePath = path
            configurations.configureEach {
                dependencies.withType<ProjectDependency>().configureEach {
                    // AGP adds the module's own project (tests depend on main).
                    // Only other features fail.
                    check(path == featurePath || !path.startsWith(":feature:")) {
                        "$featurePath must not depend on $path. Features are independent; " +
                            "move shared code into a :core module and navigate " +
                            "through keys in :app."
                    }
                }
            }
        }
    }
}

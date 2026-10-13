import com.zen.foundation.buildlogic.JAVA_VERSION
import com.zen.foundation.buildlogic.configureKotlin
import com.zen.foundation.buildlogic.library
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin module with no Android dependency (for example :core:model). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "com.android.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JAVA_VERSION
                targetCompatibility = JAVA_VERSION
            }
            configureKotlin()

            dependencies {
                "testImplementation"(libs.library("junit4"))
                "testImplementation"(libs.library("truth"))
            }
        }
    }
}

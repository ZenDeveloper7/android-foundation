import com.android.build.api.dsl.LibraryExtension
import com.zen.foundation.buildlogic.configureKotlinAndroid
import com.zen.foundation.buildlogic.intVersion
import com.zen.foundation.buildlogic.library
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.intVersion("targetSdk")
                lint.targetSdk = libs.intVersion("targetSdk")
            }

            dependencies {
                "testImplementation"(libs.library("junit4"))
                "testImplementation"(libs.library("truth"))
                "testImplementation"(libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}

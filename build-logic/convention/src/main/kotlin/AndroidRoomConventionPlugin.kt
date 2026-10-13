import androidx.room.gradle.RoomExtension
import com.zen.foundation.buildlogic.library
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room with KSP and exported schemas (checked in, so migrations can be tested). */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.library("androidx-room-runtime"))
                "ksp"(libs.library("androidx-room-compiler"))
                "testImplementation"(libs.library("androidx-room-testing"))
            }
        }
    }
}

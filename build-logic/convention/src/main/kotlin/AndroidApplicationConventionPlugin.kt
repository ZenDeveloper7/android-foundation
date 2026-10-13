import com.android.build.api.dsl.ApplicationExtension
import com.zen.foundation.buildlogic.configureKotlinAndroid
import com.zen.foundation.buildlogic.intVersion
import com.zen.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.intVersion("targetSdk")
                buildFeatures.buildConfig = true
            }
        }
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

val externalBuildRoot = providers.gradleProperty("dailyFlow.buildRoot").orNull

externalBuildRoot?.let { root ->
    layout.buildDirectory.set(file("$root/root"))
}

subprojects {
    externalBuildRoot?.let { root ->
        val projectDirectory = path.trim(':').replace(':', '/')
        layout.buildDirectory.set(file("$root/$projectDirectory"))
    }

    afterEvaluate {
        extensions.findByType<com.android.build.gradle.LibraryExtension>()?.apply {
            lint {
                disable += "NullSafeMutableLiveData"
            }
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        }
    }
}

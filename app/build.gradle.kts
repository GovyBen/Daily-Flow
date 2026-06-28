import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.mhss.app.mybrain"
    compileSdk = 36

    // --- Release signing config (DF-802) ---
    // Credentials are read from gradle properties which can be supplied via:
    //   • local.properties: dailyFlow.keystorePath, dailyFlow.keystorePassword, etc.
    //   • Environment variables: ORG_GRADLE_PROJECT_dailyFlow.keystorePath, etc.
    //   • Command line: -PdailyFlow.keystorePath=...
    // If no credentials are provided, the release build produces an unsigned APK.
    val releaseKeystorePath: String? = findProperty("dailyFlow.keystorePath") as String?
    val releaseKeystorePassword: String? = findProperty("dailyFlow.keystorePassword") as String?
    val releaseKeyAlias: String? = findProperty("dailyFlow.keyAlias") as String?
    val releaseKeyPassword: String? = findProperty("dailyFlow.keyPassword") as String?

    signingConfigs {
        create("release") {
            storeFile = releaseKeystorePath?.let { rootProject.file(it) }
            storePassword = releaseKeystorePassword ?: ""
            keyAlias = releaseKeyAlias ?: ""
            keyPassword = releaseKeyPassword ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.dailyflow.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "0.11.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only apply signing if keystore path is provided; otherwise unsigned
            signingConfig = if (releaseKeystorePath != null) signingConfigs.getByName("release") else null
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            isDebuggable = true
            resValue("string", "app_name", "Daily Flow Debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
        }
    }
    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
    lint {
        disable.add("MissingTranslation")
        disable.add("NullSafeMutableLiveData")
    }
}

dependencies {

    implementation(project(":notes:presentation"))
    implementation(project(":tasks:presentation"))
    implementation(project(":bookmarks:presentation"))
    implementation(project(":calendar:presentation"))
    implementation(project(":diary:presentation"))
    implementation(project(":settings:presentation"))
    implementation(project(":ai:presentation"))
    implementation(project(":daily"))

    implementation(project(":notes:data"))
    implementation(project(":tasks:data"))
    implementation(project(":bookmarks:data"))
    implementation(project(":diary:data"))
    implementation(project(":calendar:data"))
    implementation(project(":ai:data"))
    implementation(project(":settings:data"))

    implementation(project(":tasks:domain"))
    implementation(project(":calendar:domain"))
    implementation(project(":diary:domain"))
    implementation(project(":notes:domain"))
    implementation(project(":bookmarks:domain"))

    implementation(project(":core:notification"))
    implementation(project(":core:ui"))
    implementation(project(":core:di"))
    implementation(project(":core:alarm"))
    implementation(project(":core:database"))
    implementation(project(":widget"))
    implementation(project(":core:preferences"))
    implementation(project(":core:util"))
    implementation(project(":tracking"))

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.test.junit4)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.biometric)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.koin.android)
    implementation(libs.koin.android.workmanager)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.logging)

    implementation(libs.squircle.shape)
}


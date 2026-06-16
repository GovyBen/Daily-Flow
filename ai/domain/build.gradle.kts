plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":core:preferences"))
    implementation(project(":notes:domain"))
    implementation(project(":tasks:domain"))
    implementation(project(":calendar:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    ksp(libs.koin.ksp.compiler)
}
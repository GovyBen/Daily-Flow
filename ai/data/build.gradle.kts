plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(project(":ai:domain"))
    implementation(project(":core:preferences"))

    implementation(project(":notes:domain"))
    implementation(project(":tasks:domain"))
    implementation(project(":calendar:domain"))
    implementation(project(":diary:domain"))
    implementation(project(":bookmarks:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.cio)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koog.agents)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.mockwebserver)
}

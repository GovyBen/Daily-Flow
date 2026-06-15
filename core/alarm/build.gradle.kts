plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    ksp(libs.koin.ksp.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}

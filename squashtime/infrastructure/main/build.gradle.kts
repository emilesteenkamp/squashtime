plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.devtools.ksp)
}

dependencies {
    api(projects.squashtime.application)

    implementation(projects.squashtime.peripheral.filesystem)
    implementation(projects.squashtime.peripheral.squashcity)

    implementation(libs.kaml)
    implementation(libs.kotlinlogging)
    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.skrapeit)
    implementation(libs.slf4j.simple)

    ksp(libs.kotlininject.compiler.ksp)
}

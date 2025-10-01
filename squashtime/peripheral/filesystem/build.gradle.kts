plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    implementation(projects.squashtime.application)

    implementation(libs.kaml)
    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinx.serialization.core.jvm)

    testImplementation(libs.kotlin.test.junit)
}

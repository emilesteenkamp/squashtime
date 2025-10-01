plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.squashtime.application)

    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinlogging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.skrapeit)

    testImplementation(libs.kotlin.test.junit)
}

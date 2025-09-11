plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":squashtime:application"))

    implementation(libs.kaml)
    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinx.serialization.core.jvm)

    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(21)
}

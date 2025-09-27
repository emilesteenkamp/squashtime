plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":squashtime:workflow"))

    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinlogging)

    testImplementation(project(":squashtime:infrastructure:test"))

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

kotlin {
    jvmToolchain(21)
}

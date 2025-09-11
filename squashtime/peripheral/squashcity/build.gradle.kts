plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":squashtime:application"))

    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinlogging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.skrapeit)

    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(21)
}


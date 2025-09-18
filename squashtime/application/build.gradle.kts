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

    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(21)
}
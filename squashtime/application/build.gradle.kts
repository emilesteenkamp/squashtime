plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinlogging)

    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(24)
}
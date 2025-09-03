plugins {
    kotlin("jvm") version "2.2.0"
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.skrapeit)
    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(24)
}
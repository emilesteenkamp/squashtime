plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":squashtime:application"))
    implementation(project(":squashtime:peripheral:filesystem"))
    implementation(project(":squashtime:peripheral:squashcity"))

    implementation(libs.kaml)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.skrapeit)
    implementation(libs.slf4j.simple)
}

kotlin {
    jvmToolchain(24)
}
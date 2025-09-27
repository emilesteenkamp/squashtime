plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.devtools.ksp)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":squashtime:application"))

    implementation(libs.kotlininject.runtime)
    implementation(libs.slf4j.simple)

    ksp(libs.kotlininject.compiler.ksp)
}

kotlin {
    jvmToolchain(21)
}

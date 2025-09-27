plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.devtools.ksp)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotest.assertions.core)
    api(libs.kotlin.test.junit)
    api(libs.kotlinx.coroutines.test)

    implementation(project(":squashtime:application"))

    implementation(libs.kotlininject.runtime)
    implementation(libs.slf4j.simple)

    ksp(libs.kotlininject.compiler.ksp)
}

kotlin {
    jvmToolchain(21)
}

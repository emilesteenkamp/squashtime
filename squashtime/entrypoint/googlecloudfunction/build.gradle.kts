plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

val invoker: Configuration by configurations.creating

dependencies {
    implementation(project(":squashtime:application"))
    implementation(project(":squashtime:peripheral:filesystem"))
    implementation(project(":squashtime:peripheral:squashcity"))

    implementation(libs.google.cloud.function.framework.api)
    implementation(libs.kaml)
    implementation(libs.kotlinlogging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.skrapeit)
    implementation(libs.slf4j.simple)

    invoker(libs.google.cloud.function.invoker)
}

kotlin {
    jvmToolchain(24)
}

tasks.register<JavaExec>("runFunction") {
    mainClass.set("com.google.cloud.functions.invoker.runner.Invoker")
    classpath = invoker
    inputs.files(configurations.runtimeClasspath, sourceSets.main.get().output)
    args(
        "--target", "me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction.ReserveCourtFunctionEntryPoint",
        "--port", "8080"
    )
    doFirst {
        args(
            "--classpath",
            files(configurations.runtimeClasspath, sourceSets.main.get().output).asPath
        )
    }
}
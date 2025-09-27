plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.cloud.jib)
}

group = "me.emilesteenkamp"

repositories {
    mavenCentral()
}

val invoker: Configuration by configurations.creating

dependencies {
    implementation(project(":squashtime:application"))
    implementation(project(":squashtime:infrastructure:main"))

    implementation(libs.google.cloud.function.framework.api)
    implementation(libs.google.cloud.function.invoker)
    implementation(libs.kotlinlogging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    invoker(libs.google.cloud.function.invoker)
}

kotlin {
    jvmToolchain(21)
}

tasks.register<JavaExec>("runFunction") {
    mainClass.set("com.google.cloud.functions.invoker.runner.Invoker")
    classpath = invoker
    inputs.files(configurations.runtimeClasspath, sourceSets.main.get().output)
    args(
        "--target",
        "me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction.reservecourt.atdatetime.ReserveCourtAtDateTimeFunctionEntryPoint",
        "--port",
        "8080",
    )
    doFirst {
        args(
            "--classpath",
            files(configurations.runtimeClasspath, sourceSets.main.get().output).asPath,
        )
    }
}

jib {
    from {
        image = "europe-west4-docker.pkg.dev/serverless-runtimes/google-22/runtimes/java21"
    }
    to {
        image = "europe-west4-docker.pkg.dev/squash-time-471311/squash-time/reserve-court-at-date-time:latest"
    }
    container {
        mainClass = "com.google.cloud.functions.invoker.runner.Invoker"
        ports = listOf("8080")
        environment =
            mapOf(
                "FUNCTION_TARGET" to
                    "me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction.reservecourt.atdatetime.ReserveCourtAtDateTimeFunctionEntryPoint",
            )
    }
}

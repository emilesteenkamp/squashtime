plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.devtools.ksp)
}

dependencies {
    api(projects.squashtime.peripheral.mock)

    api(libs.kotest.assertions.core)
    api(libs.kotlin.test.junit)
    api(libs.kotlinx.coroutines.test)

    implementation(projects.squashtime.application)
    implementation(projects.squashtime.infrastructure.scope.testscoped)

    implementation(libs.kotlininject.runtime)
    implementation(libs.slf4j.simple)

    ksp(libs.kotlininject.compiler.ksp)
}

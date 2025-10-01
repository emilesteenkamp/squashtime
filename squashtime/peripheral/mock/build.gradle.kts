plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.squashtime.application)
    implementation(projects.squashtime.infrastructure.scope.testscoped)

    implementation(libs.kotlininject.runtime)
}
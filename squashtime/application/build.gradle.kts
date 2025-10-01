plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(projects.squashtime.workflow)

    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinlogging)

    testImplementation(projects.squashtime.infrastructure.test)
}

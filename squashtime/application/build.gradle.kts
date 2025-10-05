plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.emilesteenkamp.orktstrator.core)

    implementation(libs.kotlininject.runtime)
    implementation(libs.kotlinlogging)

    testImplementation(projects.squashtime.infrastructure.test)
}

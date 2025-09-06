plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "squashtime"

include(":squashtime:application")
include(":squashtime:entrypoint:googlecloudfunction")
include(":squashtime:peripheral:filesystem")
include(":squashtime:peripheral:squashcity")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "squashtime"

include("squashtime:application")
include("squashtime:infrastructure")
include("squashtime:peripheral:filesystem")
include("squashtime:peripheral:squashcity")
include("squashtime:presentation")
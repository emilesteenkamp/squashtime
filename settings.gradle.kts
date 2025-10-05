rootProject.name = "squashtime-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":squashtime:application")
include(":squashtime:entrypoint:googlecloudfunction:reservecourt:atdatetime")
include(":squashtime:entrypoint:googlecloudfunction:reservecourt:oneweeklater")
include(":squashtime:infrastructure:main")
include(":squashtime:infrastructure:test")
include(":squashtime:infrastructure:scope:testscoped")
include(":squashtime:peripheral:filesystem")
include(":squashtime:peripheral:mock")
include(":squashtime:peripheral:squashcity")

rootProject.name = "squashtime"

include(":squashtime:application")
include(":squashtime:entrypoint:googlecloudfunction:reservecourt:atdatetime")
include(":squashtime:entrypoint:googlecloudfunction:reservecourt:oneweeklater")
include(":squashtime:infrastructure:main")
include(":squashtime:infrastructure:test")
include(":squashtime:peripheral:filesystem")
include(":squashtime:peripheral:squashcity")
include(":squashtime:workflow")

package me.emilesteenkamp.squashtime.peripheral.squashcity

import it.skrape.core.htmlDocument
import kotlin.test.Test
import kotlin.test.fail

class SquashCityScheduleParserTest {
    @Test
    fun `parse schedule`() {
        htmlDocument(readFileContents(SQUASH_CITY_SCHEDULE)) {
            with(SquashCityScheduleParser) {
                parseSchedule()
            }
        }
    }

    companion object {
        private const val SQUASH_CITY_SCHEDULE = "/squashcity/squashcity-schedule.html"

        fun readFileContents(fileName: String): String {
            val resource =
                SquashCityScheduleParserTest::class.java.getResource(fileName)
                    ?: fail("Test resource not found $fileName")
            return resource.readText(Charsets.UTF_8)
        }
    }
}

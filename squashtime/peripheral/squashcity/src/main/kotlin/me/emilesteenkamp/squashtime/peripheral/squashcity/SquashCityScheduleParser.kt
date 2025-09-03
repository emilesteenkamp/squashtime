package me.emilesteenkamp.squashtime.peripheral.squashcity

import it.skrape.selects.Doc
import it.skrape.selects.html5.table
import it.skrape.selects.html5.td
import it.skrape.selects.html5.tr
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Schedule
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import java.time.LocalTime
import kotlin.collections.plus

object SquashCityScheduleParser {
    fun Doc.parseSchedule(): Schedule = table {
        findLast {
            tr {
                findAll {
                    map { trDoc ->
                        val documentTimeslotIdentifier = trDoc.attributes["utc"] ?: return@map null
                        val documentTimeslotTime = trDoc.attributes["data-time"] ?: return@map null
                        val timeslotIdentifier = Timeslot.Identifier(documentTimeslotIdentifier)
                        val timeslotTime = LocalTime.parse(documentTimeslotTime)
                        trDoc.td {
                            findAll {
                                map { tdDoc ->
                                    val documentCourtIdentifier = tdDoc.attributes["slot"] ?: return@map null
                                    val documentTimeslotStatus = tdDoc.attributes["type"] ?: ""
                                    val courtIdentifier = CourtIdentifier(documentCourtIdentifier)
                                    val timeslotStatus = when (documentTimeslotStatus) {
                                        "free" -> Timeslot.Status.FREE
                                        "taken" -> Timeslot.Status.TAKEN
                                        else -> Timeslot.Status.CLOSED
                                    }
                                    courtIdentifier to Timeslot(
                                        identifier = timeslotIdentifier,
                                        time = timeslotTime,
                                        status = timeslotStatus
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        .filterNotNull()
        .flatten()
        .filterNotNull()
        .fold(emptyMap<CourtIdentifier, List<Timeslot>>()) { acc, it ->
            acc + (it.first to ((acc[it.first] ?: emptyList()) + it.second))
        }
        .mapValues {
            it.value.sortedBy { timeslot -> timeslot.time }
        }
}
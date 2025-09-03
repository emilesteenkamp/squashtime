package me.emilesteenkamp.squashtime.application.domain

import java.time.LocalTime

data class Timeslot(
    val identifier: Identifier,
    val time: LocalTime,
    val status: Status
) {
    @JvmInline
    value class Identifier(val value: String)

    enum class Status {
        FREE,
        CLOSED,
        TAKEN
    }
}
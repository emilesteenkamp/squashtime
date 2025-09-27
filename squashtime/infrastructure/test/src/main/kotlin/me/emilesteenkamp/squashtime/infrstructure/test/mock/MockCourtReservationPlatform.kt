package me.emilesteenkamp.squashtime.infrstructure.test.mock

import java.time.LocalDate
import java.time.LocalTime
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform

object MockCourtReservationPlatform : CourtReservationPlatform {
    val courtIdentifierSet = setOf(
        CourtIdentifier("1"),
        CourtIdentifier("2"),
        CourtIdentifier("3"),
        CourtIdentifier("4"),
        CourtIdentifier("5")
    )
    val timeList = listOf(
        LocalTime.of(/* hour = */ 8, /* minute = */ 0),
        LocalTime.of(/* hour = */ 9, /* minute = */ 0),
        LocalTime.of(/* hour = */ 10, /* minute = */ 0),
        LocalTime.of(/* hour = */ 11, /* minute = */ 0),
        LocalTime.of(/* hour = */ 12, /* minute = */ 0),
        LocalTime.of(/* hour = */ 13, /* minute = */ 0),
        LocalTime.of(/* hour = */ 14, /* minute = */ 0),
        LocalTime.of(/* hour = */ 15, /* minute = */ 0),
        LocalTime.of(/* hour = */ 16, /* minute = */ 0),
        LocalTime.of(/* hour = */ 18, /* minute = */ 0),
        LocalTime.of(/* hour = */ 19, /* minute = */ 0)
    )
    val playerUserNameToPasswordMap = mapOf(
        Player.UserName("user1") to CourtReservationPlatform.Password("password1")
    )
    val dateToScheduleMap = defaultScheduleForNextSevenDays()

    override suspend fun authenticate(
        player: Player,
        password: CourtReservationPlatform.Password
    ): CourtReservationPlatform.AuthenticateResult {
        val actualPassword = playerUserNameToPasswordMap[player.userName]
            ?: return CourtReservationPlatform.AuthenticateResult.Failed

        return if (password.value == actualPassword.value) {
            CourtReservationPlatform.AuthenticateResult.Success(Session())
        } else {
            CourtReservationPlatform.AuthenticateResult.Failed
        }
    }

    class Session : CourtReservationPlatform.Session {
        override suspend fun fetchSchedule(date: LocalDate): CourtReservationPlatform.Session.FetchScheduleResult {
            val schedule = dateToScheduleMap[date] ?: return CourtReservationPlatform.Session.FetchScheduleResult.Failed
            return CourtReservationPlatform.Session.FetchScheduleResult.Success(schedule = schedule)
        }

        override suspend fun reserveCourt(
            courtIdentifier: CourtIdentifier,
            timeslot: Timeslot,
            additionalPlayerIdentifierSet: Set<Player.Identifier>,
            date: LocalDate
        ): CourtReservationPlatform.Session.ReserveCourtResult {
            val schedule = dateToScheduleMap[date] ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed
            val timeslotList = schedule[courtIdentifier] ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed
            val timeslot = timeslotList.firstOrNull { it.identifier == timeslot.identifier } ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed

            return if (timeslot.status == Timeslot.Status.FREE) {
                schedule[courtIdentifier] = timeslotList
                    .map { if (it.identifier == timeslot.identifier) it.copy(status = Timeslot.Status.TAKEN) else it }
                CourtReservationPlatform.Session.ReserveCourtResult.Success
            } else {
                CourtReservationPlatform.Session.ReserveCourtResult.Failed
            }
        }
    }

    private fun defaultScheduleForNextSevenDays() = buildMap<LocalDate, MutableSchedule> {
        (0 until 6)
            .map { dayIncrementor ->
                LocalDate.now().plusDays(dayIncrementor.toLong())
            }
            .forEach {
                put(it, defaultSchedule())
            }
    }

    private fun defaultSchedule(): MutableSchedule = mutableMapOf<CourtIdentifier, List<Timeslot>>().apply {
        courtIdentifierSet.forEach {
            put(it, defaultTimeslotList())
        }
    }

    private fun defaultTimeslotList(): List<Timeslot> = buildList {
        timeList.forEach { time ->
            add(
                Timeslot(
                    identifier = Timeslot.Identifier("${time.hour}:${time.minute}"),
                    time = time,
                    status = Timeslot.Status.FREE
                )
            )
        }
    }
}

typealias MutableSchedule = MutableMap<CourtIdentifier, List<Timeslot>>

package me.emilesteenkamp.squashtime.infrstructure.test.mock

import java.time.LocalDate
import java.time.LocalTime
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Schedule
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform

class MockCourtReservationPlatform(
    private val dataSource: DataSource
) : CourtReservationPlatform {
    override suspend fun authenticate(
        player: Player,
        password: CourtReservationPlatform.Password
    ): CourtReservationPlatform.AuthenticateResult {
        val actualPassword = dataSource.playerUserNameToPasswordMap[player.userName]
            ?: return CourtReservationPlatform.AuthenticateResult.Failed

        return if (password.value == actualPassword.value) {
            CourtReservationPlatform.AuthenticateResult.Success(Session(dataSource))
        } else {
            CourtReservationPlatform.AuthenticateResult.Failed
        }
    }

    class Session(private val dataSource: DataSource) : CourtReservationPlatform.Session {
        override suspend fun fetchSchedule(date: LocalDate): CourtReservationPlatform.Session.FetchScheduleResult {
            val mutableSchedule = dataSource.dateToMutableScheduleMap[date] ?: return CourtReservationPlatform.Session.FetchScheduleResult.Failed
            return CourtReservationPlatform.Session.FetchScheduleResult.Success(schedule = mutableSchedule.toSchedule())
        }

        override suspend fun reserveCourt(
            courtIdentifier: CourtIdentifier,
            timeslot: Timeslot,
            additionalPlayerIdentifierSet: Set<Player.Identifier>,
            date: LocalDate
        ): CourtReservationPlatform.Session.ReserveCourtResult {
            val schedule = dataSource.dateToMutableScheduleMap[date] ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed
            val timeslotList = schedule[courtIdentifier] ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed
            val timeslot = timeslotList.firstOrNull { it.identifier == timeslot.identifier } ?: return CourtReservationPlatform.Session.ReserveCourtResult.Failed

            return if (timeslot.status == Timeslot.Status.FREE) {
                timeslot.status = Timeslot.Status.TAKEN
                CourtReservationPlatform.Session.ReserveCourtResult.Success
            } else {
                CourtReservationPlatform.Session.ReserveCourtResult.Failed
            }
        }
    }

    class DataSource {
        val playerUserNameToPasswordMap = mapOf(
            Player.UserName("user1") to CourtReservationPlatform.Password("password1")
        )
        val dateToMutableScheduleMap = defaultMutableSchedulesForNextSevenDays()

        private fun defaultMutableSchedulesForNextSevenDays() = buildMap<LocalDate, MutableSchedule> {
            dateList.forEach { put(it, defaultMutableSchedule()) }
        }

        private fun defaultMutableSchedule(): MutableSchedule = buildMap {
            courtIdentifierSet.forEach { put(it, defaultMutableTimeslotList()) }
        }

        private fun defaultMutableTimeslotList(): List<MutableTimeslot> = buildList {
            timeList.forEach { time ->
                add(
                    MutableTimeslot(
                        identifier = Timeslot.Identifier("${time.hour}:${time.minute}"),
                        time = time,
                        status = Timeslot.Status.FREE
                    )
                )
            }
        }

        data class MutableTimeslot(
            val identifier: Timeslot.Identifier,
            val time: LocalTime,
            var status: Timeslot.Status,
        )

        companion object {
            private val courtIdentifierSet = setOf(
                CourtIdentifier("1"),
                CourtIdentifier("2"),
                CourtIdentifier("3"),
                CourtIdentifier("4"),
                CourtIdentifier("5")
            )
            private val dateList = (0 until 7)
                .map { dayIncrementor ->
                    LocalDate.now().plusDays(dayIncrementor.toLong())
                }
            private val timeList = listOf(
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
        }
    }

    companion object {
        fun MutableSchedule.toSchedule(): Schedule = this.mapValues { (_, mutableTimeslotList) ->
            mutableTimeslotList.map { mutableTimeslot ->
                mutableTimeslot.toTimeslot()
            }
        }

        fun DataSource.MutableTimeslot.toTimeslot(): Timeslot = Timeslot(
            identifier = this.identifier,
            time = this.time,
            status = this.status
        )
    }
}

typealias MutableSchedule = Map<CourtIdentifier, List<MockCourtReservationPlatform.DataSource.MutableTimeslot>>

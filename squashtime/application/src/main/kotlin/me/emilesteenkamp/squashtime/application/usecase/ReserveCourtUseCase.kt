package me.emilesteenkamp.squashtime.application.usecase

import java.time.LocalDateTime
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup

class ReserveCourtUseCase(
    private val courtReservationPlatform: CourtReservationPlatform,
    private val courtReservationPlatformPasswordLookup: CourtReservationPlatformPasswordLookup,
) {
    suspend operator fun invoke(
        player: Player,
        additionalPlayerIdentifierSet: Set<Player.Identifier>,
        requestedDateTime: LocalDateTime,
    ) {
        require(additionalPlayerIdentifierSet.size in 1..3) {
            "Exactly 1-3 additional players required, but ${additionalPlayerIdentifierSet.size} was given."
        }

        val requestedDate = requestedDateTime.toLocalDate()
        val password = courtReservationPlatformPasswordLookup.find(player.userName)

        require(password != null) {
            "Could not find password for player: $player"
        }

        val session = when (val result = courtReservationPlatform.authenticate(player, password)) {
            is CourtReservationPlatform.AuthenticateResult.Success -> result.session
            CourtReservationPlatform.AuthenticateResult.Failed -> error("Authentication failed.")
        }

        val schedule = when (val result = session.fetchSchedule(date = requestedDate)) {
            is CourtReservationPlatform.Session.FetchScheduleResult.Success -> result.schedule
            CourtReservationPlatform.Session.FetchScheduleResult.Failed -> error("Failed to fetch schedule.")
        }

        val courtTimeslot = schedule.firstNotNullOfOrNull { (courtIdentifier, timeslotList) ->
            val timeslot = timeslotList.find { it.time.compareTo(requestedDateTime.toLocalTime()) == 0 }
            if (timeslot?.status == Timeslot.Status.FREE) CourtTimeslot(courtIdentifier, timeslot) else null
        }

        require(courtTimeslot != null) {
            "No timeslot found."
        }

        when (
            session.reserveCourt(
                courtIdentifier = courtTimeslot.courtIdentifier,
                timeslot = courtTimeslot.timeslot,
                additionalPlayerIdentifierSet = additionalPlayerIdentifierSet,
                date = requestedDate
            )
        ) {
            CourtReservationPlatform.Session.ReserveCourtResult.Success -> {}
            CourtReservationPlatform.Session.ReserveCourtResult.Failed -> error("Failed to reserve court.")
        }
    }

    private data class CourtTimeslot(
        val courtIdentifier: CourtIdentifier,
        val timeslot: Timeslot
    )
}
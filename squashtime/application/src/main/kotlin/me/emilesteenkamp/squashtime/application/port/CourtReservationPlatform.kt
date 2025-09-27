package me.emilesteenkamp.squashtime.application.port

import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Schedule
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import java.time.LocalDate

interface CourtReservationPlatform {
    suspend fun authenticate(
        player: Player,
        password: Password,
    ): AuthenticateResult

    sealed interface AuthenticateResult {
        data class Success(
            val session: Session,
        ) : AuthenticateResult

        data object Failed : AuthenticateResult
    }

    interface Session {
        suspend fun fetchSchedule(date: LocalDate): FetchScheduleResult

        sealed interface FetchScheduleResult {
            data class Success(
                val schedule: Schedule,
            ) : FetchScheduleResult

            data object Failed : FetchScheduleResult
        }

        suspend fun reserveCourt(
            courtIdentifier: CourtIdentifier,
            timeslot: Timeslot,
            additionalPlayerIdentifierSet: Set<Player.Identifier>,
            date: LocalDate,
        ): ReserveCourtResult

        sealed interface ReserveCourtResult {
            data object Success : ReserveCourtResult

            data object Failed : ReserveCourtResult
        }
    }

    @JvmInline
    value class Password(
        val value: String,
    ) {
        override fun toString(): String = "Password(value=${"*".repeat(value.length)})"
    }
}

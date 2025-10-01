package me.emilesteenkamp.squashtime.application.usecase

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.infrstructure.test.api.runWithTestInfrastructure
import org.junit.Test
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ReserveCourtUseCaseTest {
    @Test
    fun `Reserving a court is successful`() =
        runWithTestInfrastructure {
            // Given.
            val input =
                ReserveCourtUseCase.State.Transient(
                    player =
                        Player(
                            identifier = Player.Identifier(Uuid.random().toHexString()),
                            userName = Player.UserName("user1"),
                        ),
                    additionalPlayerIdentifierSet = setOf(Player.Identifier(Uuid.random().toHexString())),
                    requestedDateTime =
                        LocalDate
                            .now()
                            .plusDays(5)
                            .atTime(/* hour = */ 14, /* minute = */ 0),
                )

            // When.
            val output = reserveCourtUseCase(input)

            // Then.
            val success = output.shouldBeInstanceOf<ReserveCourtUseCase.State.Final.Success>()
            mockCourtReservationPlatformDataSource
                .dateToMutableScheduleMap[input.requestedDateTime.toLocalDate()]
                .shouldNotBeNull()[success.bookedCourtIdentifier]
                .shouldNotBeNull()
                .find { it.time.compareTo(input.requestedDateTime.toLocalTime()) == 0 }
                .shouldNotBeNull()
                .status
                .shouldBe(Timeslot.Status.TAKEN)
        }

    @Test
    fun `Reserving a court fails if there are no fail courts available`() =
        runWithTestInfrastructure {
            // Given.
            val input =
                ReserveCourtUseCase.State.Transient(
                    player =
                        Player(
                            identifier = Player.Identifier(Uuid.random().toHexString()),
                            userName = Player.UserName("user1"),
                        ),
                    additionalPlayerIdentifierSet = setOf(Player.Identifier(Uuid.random().toHexString())),
                    requestedDateTime =
                        LocalDate
                            .now()
                            .plusDays(5)
                            .atTime(/* hour = */ 14, /* minute = */ 0),
                )
            mockCourtReservationPlatformDataSource
                .dateToMutableScheduleMap[input.requestedDateTime.toLocalDate()]
                .shouldNotBeNull()
                .forEach { (_, mutableTimeslotList) ->
                    mutableTimeslotList.map { mutableTimeslot ->
                        mutableTimeslot.status = Timeslot.Status.TAKEN
                    }
                }

            // When.
            val output = reserveCourtUseCase(input)

            // Then.
            output.shouldBeInstanceOf<ReserveCourtUseCase.State.Final.Error.NoTimeslotAvailable>()
        }

    @Test
    fun `Reserving a court fails if the player password is not found`() =
        runWithTestInfrastructure {
            // Given.
            val input =
                ReserveCourtUseCase.State.Transient(
                    player =
                        Player(
                            identifier = Player.Identifier(Uuid.random().toHexString()),
                            userName = Player.UserName("user2"),
                        ),
                    additionalPlayerIdentifierSet = setOf(Player.Identifier(Uuid.random().toHexString())),
                    requestedDateTime =
                        LocalDate
                            .now()
                            .plusDays(5)
                            .atTime(/* hour = */ 14, /* minute = */ 0),
                )

            // When.
            val output = reserveCourtUseCase(input)

            // Then.
            output.shouldBeInstanceOf<ReserveCourtUseCase.State.Final.Error.PasswordNotFound>()
        }
}

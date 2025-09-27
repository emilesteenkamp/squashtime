package me.emilesteenkamp.squashtime.application.usecase

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.infrstructure.test.TestInfrastructure
import me.emilesteenkamp.squashtime.infrstructure.test.create
import me.emilesteenkamp.squashtime.infrstructure.test.mock.MockCourtReservationPlatform
import org.junit.Test

@OptIn(ExperimentalUuidApi::class)
class ReserveCourtUseCaseTest {
    val reserveCourtUseCase = TestInfrastructure::class.create().reserveCourtUseCase

    @Test
    fun `Stub test`() = runTest {
        // Given.
        val input = ReserveCourtUseCase.State.Transient(
            player = Player(
                identifier = Player.Identifier(Uuid.random().toHexString()),
                userName = Player.UserName("user1")
            ),
            additionalPlayerIdentifierSet = setOf(Player.Identifier("")),
            requestedDateTime = LocalDate.now()
                .plusDays(5)
                .atTime(/* hour = */ 14, /* minute = */ 0)
        )

        // When.
        val output = reserveCourtUseCase(input)

        // Then.
        val success = output.shouldBeInstanceOf<ReserveCourtUseCase.State.Final.Success>()
        MockCourtReservationPlatform.dateToScheduleMap[input.requestedDateTime.toLocalDate()]
            ?.get(success.bookedCourtIdentifier)
            ?.find { it.time.compareTo(input.requestedDateTime.toLocalTime()) == 0 }
            .shouldNotBeNull()
            .status
            .shouldBe(Timeslot.Status.TAKEN)
    }
}
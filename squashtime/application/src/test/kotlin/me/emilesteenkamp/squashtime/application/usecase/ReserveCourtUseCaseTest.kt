package me.emilesteenkamp.squashtime.application.usecase

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.infrstructure.test.TestInfrastructure
import me.emilesteenkamp.squashtime.infrstructure.test.create
import org.junit.Test

@OptIn(ExperimentalUuidApi::class)
class ReserveCourtUseCaseTest {
    @Test
    fun `Reserve a court`() = runTest {
        // Given.
        val testInfrastructure = TestInfrastructure::class.create()
        val reserveCourtUseCase = testInfrastructure.reserveCourtUseCase
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
        testInfrastructure
            .mockCourtReservationPlatformDataSource
            .dateToScheduleMap[input.requestedDateTime.toLocalDate()]
            .shouldNotBeNull()[success.bookedCourtIdentifier]
            .shouldNotBeNull()
            .find { it.time.compareTo(input.requestedDateTime.toLocalTime()) == 0 }
            .shouldNotBeNull()
    }
}
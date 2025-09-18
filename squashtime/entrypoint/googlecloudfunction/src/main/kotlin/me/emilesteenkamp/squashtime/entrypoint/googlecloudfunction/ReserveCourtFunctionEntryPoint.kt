package me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction

import com.google.cloud.functions.CloudEventsFunction
import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.cloudevents.CloudEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.infrastructure.infrastructure

@Suppress("Unused")
class ReserveCourtFunctionEntryPoint : HttpFunction by ReserveCourtFunction(
    reserveCourtUseCase = infrastructure.reserveCourtUseCase,
    json = infrastructure.json
)

class ReserveCourtFunction(
    private val reserveCourtUseCase: ReserveCourtUseCase,
    private val json: Json,
) : HttpFunction, CloudEventsFunction {
    override fun service(
        request: HttpRequest,
        response: HttpResponse
    ) = runBlocking {
        val reserveCourtRequestSerial = request.decodeToSerial<ReserveCourtRequestSerial>()

        logger.info { "Accepted request with serial $reserveCourtRequestSerial" }

        when (
            reserveCourtUseCase.invoke(
                ReserveCourtUseCase.State.Transient(
                    player = reserveCourtRequestSerial.player.toPlayer(),
                    additionalPlayerIdentifierSet = reserveCourtRequestSerial.additionalPlayerIdentifierList.map { Player.Identifier(it) }.toSet(),
                    requestedDateTime = reserveCourtRequestSerial.dateTime.toLocalDateTime()
                )
            )
        ) {
            ReserveCourtUseCase.State.Final.Success -> response.setStatusCode(200)
            ReserveCourtUseCase.State.Final.Error.AuthenticationFailed,
            ReserveCourtUseCase.State.Final.Error.CourtReservationFailed,
            ReserveCourtUseCase.State.Final.Error.FailedToFetchSchedule,
            ReserveCourtUseCase.State.Final.Error.InvalidNumberOfAdditionalPlayers,
            ReserveCourtUseCase.State.Final.Error.NoTimeslotAvailable,
            ReserveCourtUseCase.State.Final.Error.PasswordNotFound -> response.setStatusCode(400)
        }
    }

    private inline fun <reified SERIAL> HttpRequest.decodeToSerial(): SERIAL =
        reader.readText().let { json.decodeFromString<SERIAL>(it) }

    override fun accept(event: CloudEvent) = runBlocking {
        val reserveCourtRequestSerial = event.decodeToSerial<ReserveCourtRequestSerial>()

        logger.info { "Accepted event with serial $reserveCourtRequestSerial" }

        when (
            reserveCourtUseCase.invoke(
                ReserveCourtUseCase.State.Transient(
                    player = reserveCourtRequestSerial.player.toPlayer(),
                    additionalPlayerIdentifierSet = reserveCourtRequestSerial.additionalPlayerIdentifierList.map { Player.Identifier(it) }.toSet(),
                    requestedDateTime = reserveCourtRequestSerial.dateTime.toLocalDateTime()
                )
            )
        ) {
            ReserveCourtUseCase.State.Final.Success -> {}
            ReserveCourtUseCase.State.Final.Error.AuthenticationFailed,
            ReserveCourtUseCase.State.Final.Error.CourtReservationFailed,
            ReserveCourtUseCase.State.Final.Error.FailedToFetchSchedule,
            ReserveCourtUseCase.State.Final.Error.InvalidNumberOfAdditionalPlayers,
            ReserveCourtUseCase.State.Final.Error.NoTimeslotAvailable,
            ReserveCourtUseCase.State.Final.Error.PasswordNotFound -> error("Failed to reserve court.")
        }
    }

    private inline fun <reified SERIAL> CloudEvent.decodeToSerial(): SERIAL =
        data?.toBytes()?.decodeToString()
            ?.let { json.decodeFromString<SERIAL>(it) }
            ?: error("Cloud event has no data.")

    private fun ReserveCourtRequestSerial.PlayerSerial.toPlayer(): Player {
        return Player(
            identifier = Player.Identifier(this.identifier),
            userName = Player.UserName(this.userName)
        )
    }

    private fun ReserveCourtRequestSerial.DateTimeSerial.toLocalDateTime(): LocalDateTime = LocalDateTime
        .of(
            /* year = */ this.year,
            /* month = */ this.month,
            /* dayOfMonth = */ this.day,
            /* hour = */ this.hour,
            /* minute = */ this.minute,
            /* second = */ this.second
        )

    @Serializable
    data class ReserveCourtRequestSerial(
        val player: PlayerSerial,
        val additionalPlayerIdentifierList: List<String>,
        val dateTime: DateTimeSerial
    ) {
        @Serializable
        data class PlayerSerial(
            val identifier: String,
            val userName: String
        )

        @Serializable
        data class DateTimeSerial(
            val year: Int,
            val month: Int,
            val day: Int,
            val hour: Int,
            val minute: Int,
            val second: Int
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
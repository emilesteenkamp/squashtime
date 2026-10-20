package me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction.reservecourt.atdatetime

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
import me.emilesteenkamp.squashtime.infrastructure.main.infrastructure

@Suppress("Unused")
class ReserveCourtAtDateTimeFunctionEntryPoint :
    HttpFunction by ReserveCourtFunction(
        reserveCourtUseCase = infrastructure.reserveCourtUseCase,
        json = infrastructure.json,
    )

class ReserveCourtFunction(
    private val reserveCourtUseCase: ReserveCourtUseCase,
    private val json: Json,
) : HttpFunction,
    CloudEventsFunction {
    override fun service(
        request: HttpRequest,
        response: HttpResponse,
    ): Unit = runBlocking {
        val reserveCourtRequestSerial = request.decodeToSerial<ReserveCourtRequestSerial>()

        logger.info { "Accepted request with serial $reserveCourtRequestSerial" }

        val output =
            reserveCourtUseCase.invoke(
                ReserveCourtUseCase.Input(
                    player = reserveCourtRequestSerial.player.toPlayer(),
                    additionalPlayerIdentifierSet =
                        reserveCourtRequestSerial.additionalPlayerIdentifierList
                            .map {
                                Player.Identifier(
                                    it,
                                )
                            }.toSet(),
                    requestedDateTime = reserveCourtRequestSerial.dateTime.toLocalDateTime(),
                ),
            )

        when (output) {
            is ReserveCourtUseCase.Output.Success -> response.setStatusCode(200)
            is ReserveCourtUseCase.Output.Failure -> {
                val statusCode =
                    when (output) {
                        ReserveCourtUseCase.Output.Failure.InvalidNumberOfAdditionalPlayers,
                        ReserveCourtUseCase.Output.Failure.PasswordNotFound,
                        ReserveCourtUseCase.Output.Failure.NoTimeslotAvailable,
                        -> 400
                        ReserveCourtUseCase.Output.Failure.AuthenticationFailed,
                        ReserveCourtUseCase.Output.Failure.FailedToFetchSchedule,
                        ReserveCourtUseCase.Output.Failure.CourtReservationFailed,
                        -> 500
                    }

                if (statusCode == 400) logger.warn { output.message } else logger.error { output.message }
                response.setStatusCode(statusCode)
            }
        }
    }

    private inline fun <reified SERIAL> HttpRequest.decodeToSerial(): SERIAL = reader.readText().let { json.decodeFromString<SERIAL>(it) }

    override fun accept(event: CloudEvent): Unit =
        runBlocking {
            val reserveCourtRequestSerial = event.decodeToSerial<ReserveCourtRequestSerial>()

            logger.info { "Accepted event with serial $reserveCourtRequestSerial" }

            val output =
                reserveCourtUseCase.invoke(
                    ReserveCourtUseCase.Input(
                        player = reserveCourtRequestSerial.player.toPlayer(),
                        additionalPlayerIdentifierSet =
                            reserveCourtRequestSerial.additionalPlayerIdentifierList
                                .map {
                                    Player.Identifier(
                                        it,
                                    )
                                }.toSet(),
                        requestedDateTime = reserveCourtRequestSerial.dateTime.toLocalDateTime(),
                    ),
                )

            if (output is ReserveCourtUseCase.Output.Failure) {
                logger.warn { output.message }
            }
        }

    private inline fun <reified SERIAL> CloudEvent.decodeToSerial(): SERIAL =
        data
            ?.toBytes()
            ?.decodeToString()
            ?.let { json.decodeFromString<SERIAL>(it) }
            ?: error("Cloud event has no data.")

    private val ReserveCourtUseCase.Output.Failure.message: String
        get() = when (this) {
            ReserveCourtUseCase.Output.Failure.InvalidNumberOfAdditionalPlayers -> "Invalid number of additional players."
            ReserveCourtUseCase.Output.Failure.PasswordNotFound -> "Password not found."
            ReserveCourtUseCase.Output.Failure.NoTimeslotAvailable -> "No timeslot available."
            ReserveCourtUseCase.Output.Failure.AuthenticationFailed -> "Court reservation platform authentication failed."
            ReserveCourtUseCase.Output.Failure.FailedToFetchSchedule -> "Failed to fetch schedule."
            ReserveCourtUseCase.Output.Failure.CourtReservationFailed -> "Court reservation failed."
        }

    private fun ReserveCourtRequestSerial.PlayerSerial.toPlayer(): Player =
        Player(
            identifier = Player.Identifier(this.identifier),
            userName = Player.UserName(this.userName),
        )

    private fun ReserveCourtRequestSerial.DateTimeSerial.toLocalDateTime(): LocalDateTime =
        LocalDateTime
            .of(
                // year =
                this.year,
                // month =
                this.month,
                // dayOfMonth =
                this.day,
                // hour =
                this.hour,
                // minute =
                this.minute,
                // second =
                this.second,
            )

    @Serializable
    data class ReserveCourtRequestSerial(
        val player: PlayerSerial,
        val additionalPlayerIdentifierList: List<String>,
        val dateTime: DateTimeSerial,
    ) {
        @Serializable
        data class PlayerSerial(
            val identifier: String,
            val userName: String,
        )

        @Serializable
        data class DateTimeSerial(
            val year: Int,
            val month: Int,
            val day: Int,
            val hour: Int,
            val minute: Int,
            val second: Int,
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

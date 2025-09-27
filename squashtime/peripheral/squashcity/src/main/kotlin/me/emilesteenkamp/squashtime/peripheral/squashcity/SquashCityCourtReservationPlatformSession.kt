package me.emilesteenkamp.squashtime.peripheral.squashcity

import io.github.oshai.kotlinlogging.KotlinLogging
import it.skrape.core.document
import it.skrape.fetcher.Cookie
import it.skrape.fetcher.Method
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.body
import it.skrape.selects.html5.form
import it.skrape.selects.html5.input
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SquashCityCourtReservationPlatformSession(
    private val authenticatedPlayerIdentifier: Player.Identifier,
    private val authenticationCookie: Cookie,
    private val fetcher: NonBlockingFetcher<Request>,
    private val scheduleParser: SquashCityScheduleParser,
) : CourtReservationPlatform.Session {
    override suspend fun fetchSchedule(date: LocalDate) =
        skrape(fetcher) {
            request {
                url =
                    "$BAANRESERVEREN_URL/reservations/${date.year}-${date.month.value}-${date.dayOfMonth}/sport/$SPORT_SQUASH_ID"
                setAuthenticationCookie(authenticationCookie)
            }
            response {
                if (responseStatus.component1() != 200) {
                    logger.warnUnexpectedResponseCode(expected = 200, actual = responseStatus.component1())
                    return@response CourtReservationPlatform.Session.FetchScheduleResult.Failed
                }

                CourtReservationPlatform.Session.FetchScheduleResult.Success(
                    schedule = with(scheduleParser) { document.parseSchedule() },
                )
            }
        }

    override suspend fun reserveCourt(
        courtIdentifier: CourtIdentifier,
        timeslot: Timeslot,
        additionalPlayerIdentifierSet: Set<Player.Identifier>,
        date: LocalDate,
    ): CourtReservationPlatform.Session.ReserveCourtResult {
        val reservationToken =
            when (
                val result =
                    retrieveReservationToken(
                        courtIdentifier = courtIdentifier,
                        timeslot = timeslot,
                    )
            ) {
                is RetrieveReservationTokenResult.Success -> result.reservationToken
                RetrieveReservationTokenResult.Failed,
                RetrieveReservationTokenResult.TokenNotFound,
                -> return CourtReservationPlatform.Session.ReserveCourtResult.Failed
            }

        return when (
            confirmReservation(
                courtIdentifier = courtIdentifier,
                timeslot = timeslot,
                additionalPlayerIdentifierSet = additionalPlayerIdentifierSet,
                date = date,
                reservationToken = reservationToken,
            )
        ) {
            ConfirmReservationResult.Success -> CourtReservationPlatform.Session.ReserveCourtResult.Success
            ConfirmReservationResult.Failed -> CourtReservationPlatform.Session.ReserveCourtResult.Failed
        }
    }

    private suspend fun retrieveReservationToken(
        courtIdentifier: CourtIdentifier,
        timeslot: Timeslot,
    ): RetrieveReservationTokenResult =
        skrape(fetcher) {
            request {
                url = "$BAANRESERVEREN_URL/reservations/make/${courtIdentifier.value}/${timeslot.identifier.value}"
                setAuthenticationCookie(authenticationCookie)
            }
            response {
                if (responseStatus.component1() != 200) {
                    logger.warnUnexpectedResponseCode(expected = 200, actual = responseStatus.component1())
                    return@response RetrieveReservationTokenResult.Failed
                }
                document.body {
                    form {
                        input {
                            findFirst {
                                attributes["value"]
                                    ?.let { RetrieveReservationTokenResult.Success(reservationToken = it) }
                                    ?: RetrieveReservationTokenResult.TokenNotFound
                            }
                        }
                    }
                }
            }
        }

    private sealed interface RetrieveReservationTokenResult {
        data class Success(
            val reservationToken: String,
        ) : RetrieveReservationTokenResult

        data object TokenNotFound : RetrieveReservationTokenResult

        data object Failed : RetrieveReservationTokenResult
    }

    private suspend fun confirmReservation(
        courtIdentifier: CourtIdentifier,
        timeslot: Timeslot,
        additionalPlayerIdentifierSet: Set<Player.Identifier>,
        date: LocalDate,
        reservationToken: String,
    ): ConfirmReservationResult {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return skrape(fetcher) {
            request {
                url = "$BAANRESERVEREN_URL/reservations/confirm"
                method = Method.POST
                setAuthenticationCookie(authenticationCookie)
                setCORSHeaders()
                body {
                    form {
                        "_token" to reservationToken
                        "resource_id" to courtIdentifier.value
                        "date" to date.format(dateFormatter)
                        "start_time" to timeslot.time.format(timeFormatter)
                        "end_time" to timeslot.time.plusMinutes(45).format(timeFormatter)
                        "players[1]" to authenticatedPlayerIdentifier.value
                        additionalPlayerIdentifierSet.forEachIndexed { index, additionalPlayerIdentifier ->
                            "players[${2 + index}]" to additionalPlayerIdentifier.value
                        }
                        "confirmed" to 1
                    }
                }
            }
            response {
                if (responseStatus.component1() != 200) {
                    logger.warnUnexpectedResponseCode(expected = 200, actual = responseStatus.component1())
                    return@response ConfirmReservationResult.Failed
                }

                ConfirmReservationResult.Success
            }
        }
    }

    sealed interface ConfirmReservationResult {
        data object Success : ConfirmReservationResult

        data object Failed : ConfirmReservationResult
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

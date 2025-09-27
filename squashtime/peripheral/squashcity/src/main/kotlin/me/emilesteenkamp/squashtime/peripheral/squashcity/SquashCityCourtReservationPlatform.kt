package me.emilesteenkamp.squashtime.peripheral.squashcity

import io.github.oshai.kotlinlogging.KotlinLogging
import it.skrape.fetcher.Method
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.tatarka.inject.annotations.Inject

class SquashCityCourtReservationPlatform
    @Inject
    constructor(
        private val fetcher: NonBlockingFetcher<Request>,
        private val sessionFactory: SquashCityCourtReservationPlatformSessionFactory,
    ) : CourtReservationPlatform {
        override suspend fun authenticate(
            player: Player,
            password: CourtReservationPlatform.Password,
        ): CourtReservationPlatform.AuthenticateResult =
            skrape(fetcher) {
                request {
                    url = "$BAANRESERVEREN_URL/auth/login"
                    method = Method.POST
                    body {
                        form {
                            "username" to player.userName.value
                            "password" to password.value
                            "goto" to "/reservations"
                        }
                    }
                }
                response {
                    if (responseStatus.component1() != 302) {
                        logger.warnUnexpectedResponseCode(expected = 302, actual = responseStatus.component1())
                        return@response CourtReservationPlatform.AuthenticateResult.Failed
                    }

                    val authenticationCookie = cookies.first()

                    CourtReservationPlatform.AuthenticateResult.Success(
                        session =
                            sessionFactory.start(
                                authenticatedPlayerIdentifier = player.identifier,
                                authenticationCookie = authenticationCookie,
                            ),
                    )
                }
            }

        companion object {
            private val logger = KotlinLogging.logger {}
        }
    }

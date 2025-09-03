package me.emilesteenkamp.squashtime.peripheral.squashcity

import it.skrape.fetcher.Cookie
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import me.emilesteenkamp.squashtime.application.domain.Player

class SquashCityCourtReservationPlatformSessionFactory(
    private val fetcher: NonBlockingFetcher<Request>,
    private val scheduleParser: SquashCityScheduleParser,
) {
    fun start(
        authenticatedPlayerIdentifier: Player.Identifier,
        authenticationCookie: Cookie
    ): SquashCityCourtReservationPlatformSession {
        return SquashCityCourtReservationPlatformSession(
            authenticatedPlayerIdentifier = authenticatedPlayerIdentifier,
            authenticationCookie = authenticationCookie,
            fetcher = fetcher,
            scheduleParser = scheduleParser
        )
    }
}
package me.emilesteenkamp.squashtime.peripheral.squashcity

import io.github.oshai.kotlinlogging.KLogger
import it.skrape.fetcher.Cookie
import it.skrape.fetcher.Request

fun Request.setAuthenticationCookie(authenticationCookie: Cookie) {
    headers = headers + ("cookie" to "${authenticationCookie.name}=${authenticationCookie.value}")
}

fun Request.setCORSHeaders() {
    headers = headers + mapOf(
        "origin" to "https://squashcity.baanreserveren.nl",
        "referer" to "https://squashcity.baanreserveren.nl/reservations",
        "sec-fetch-dest" to "empty",
        "sec-fetch-mode" to "cors",
        "sec-fetch-site" to "same-origin"
    )
}

fun KLogger.warnUnexpectedResponseCode(
    expected: Int,
    actual: Int
) {
    atWarn {
        message = "Unexpected response code."
        payload = mapOf(
            "expectedResponseCode" to expected,
            "actualResponseCode" to actual
        )
    }
}
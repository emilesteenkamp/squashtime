package me.emilesteenkamp.squashtime.infrastructure

import com.charleskorn.kaml.Yaml
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.filesystem.FileSystemCourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityCourtReservationPlatform
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityCourtReservationPlatformSessionFactory
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityScheduleParser

suspend fun main() {
    val httpClient = HttpClient(OkHttp) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
    val fetcher = HttpNonBlockingFetcher(httpClient = httpClient)
    val squashCityCourtReservationPlatformSessionFactory = SquashCityCourtReservationPlatformSessionFactory(
        fetcher = fetcher,
        scheduleParser = SquashCityScheduleParser
    )
    val squashCityCourtReservationPlatform = SquashCityCourtReservationPlatform(
        fetcher = fetcher,
        sessionFactory = squashCityCourtReservationPlatformSessionFactory
    )
    val credentialsFile = object {}::class.java.getResource(SQUASH_CITY_CREDENTIALS_LOOKUP_FILE)

    require(credentialsFile != null) {
        "No squash-city-credentials-lookup.yaml file found."
    }

    val fileSystemCourtReservationPlatformPasswordLookup = FileSystemCourtReservationPlatformPasswordLookup(
        file = credentialsFile.asJavaFile(),
        yaml = Yaml.default
    )
    val reserveCourtUseCase = ReserveCourtUseCase(
        courtReservationPlatform = squashCityCourtReservationPlatform,
        courtReservationPlatformPasswordLookup = fileSystemCourtReservationPlatformPasswordLookup
    )

    reserveCourtUseCase.invoke(
        player = Player(
            identifier = Player.Identifier("1468356"),
            userName = Player.UserName("emile@steenkamps.org")
        ),
        requestedDateTime = LocalDate
            .now()
            .plusWeeks(1)
            .atTime(LocalTime.of(/* hour = */ 14,  /* minute = */ 0)),
        additionalPlayerIdentifierSet = setOf(Player.Identifier("1201645"))
    )

    httpClient.close()
}

const val SQUASH_CITY_CREDENTIALS_LOOKUP_FILE = "/squash-city-credentials-lookup.yaml"

fun URL.asJavaFile(): File = File(this.file)

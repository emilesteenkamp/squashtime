package me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction

import com.charleskorn.kaml.Yaml
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import java.io.File
import java.net.URL
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.filesystem.FileSystemCourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityCourtReservationPlatform
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityCourtReservationPlatformSessionFactory
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityScheduleParser

val httpClient = HttpClient(OkHttp) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
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
val credentialsFile = object {}::class.java
    .getResource(SQUASH_CITY_CREDENTIALS_LOOKUP_FILE)
    ?: error("No squash-city-credentials-lookup.yaml file found.")

val fileSystemCourtReservationPlatformPasswordLookup = FileSystemCourtReservationPlatformPasswordLookup(
    file = credentialsFile.asJavaFile(),
    yaml = Yaml.default
)
val reserveCourtUseCase = ReserveCourtUseCase(
    courtReservationPlatform = squashCityCourtReservationPlatform,
    courtReservationPlatformPasswordLookup = fileSystemCourtReservationPlatformPasswordLookup
)
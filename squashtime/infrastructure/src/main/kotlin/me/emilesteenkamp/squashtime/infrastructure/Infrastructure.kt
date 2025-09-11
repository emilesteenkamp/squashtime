package me.emilesteenkamp.squashtime.infrastructure

import com.charleskorn.kaml.Yaml
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import it.skrape.fetcher.NonBlockingFetcher
import it.skrape.fetcher.Request
import kotlinx.serialization.json.Json
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.filesystem.CourtReservationPlatformCredentialsInputStream
import me.emilesteenkamp.squashtime.filesystem.FileSystemCourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityCourtReservationPlatform
import me.emilesteenkamp.squashtime.peripheral.squashcity.SquashCityScheduleParser
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

val infrastructure = Infrastructure::class.create()

@Component
abstract class Infrastructure {
    abstract val reserveCourtUseCase: ReserveCourtUseCase
    abstract val json: Json

    internal val HttpNonBlockingFetcher.bind: NonBlockingFetcher<Request>
        @Provides get() = this

    internal val SquashCityCourtReservationPlatform.bind: CourtReservationPlatform
        @Provides get() = this

    internal val FileSystemCourtReservationPlatformPasswordLookup.bind: CourtReservationPlatformPasswordLookup
        @Provides get() = this

    @Provides
    protected fun json(): Json = Json

    @Provides
    protected fun yaml(): Yaml = Yaml.default

    @Provides
    protected fun httpClient(): HttpClient = HttpClient(OkHttp) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    @Provides
    protected fun credentialsInputStream(): CourtReservationPlatformCredentialsInputStream =
        Infrastructure::class.java
        .getResource(SQUASH_CITY_CREDENTIALS_LOOKUP_FILE)
        ?.openStream()
        ?: error("No squash-city-credentials-lookup.yaml file found.")

    @Provides
    protected fun squashCityScheduleParser() = SquashCityScheduleParser
}
